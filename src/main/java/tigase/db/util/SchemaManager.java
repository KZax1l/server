/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017, "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.db.util;

import tigase.component.DSLBeanConfigurator;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.conf.ConfigBuilder;
import tigase.conf.ConfigReader;
import tigase.conf.ConfigWriter;
import tigase.conf.ConfiguratorAbstract;
import tigase.db.*;
import tigase.db.beans.*;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.beans.selector.ServerBeanSelector;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;
import tigase.kernel.core.RegistrarKernel;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.XMPPServer;
import tigase.server.monitor.MonitorRuntime;
import tigase.sys.TigaseRuntime;
import tigase.util.ClassUtilBean;
import tigase.util.DNSResolverFactory;
import tigase.util.setup.SetupHelper;
import tigase.util.ui.console.CommandlineParameter;
import tigase.util.ui.console.ParameterParser;
import tigase.util.ui.console.Task;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andrzej on 02.05.2017.
 */
public class SchemaManager {

	private static final Logger log = Logger.getLogger(SchemaManager.class.getCanonicalName());

	protected static final Class[] SUPPORTED_CLASSES = {MDPoolBean.class, MDRepositoryBean.class,
														SDRepositoryBean.class};

	private static final Comparator<SchemaInfo> SCHEMA_INFO_COMPARATOR = (o1, o2) -> {
		if (o1.getId().equals("<unknown>") || o2.getId().equals(Schema.SERVER_SCHEMA_ID))
			return 1;
		if (o2.getId().equals("<unknown>") || o1.getId().equals(Schema.SERVER_SCHEMA_ID))
			return -1;
		return o1.getId().compareTo(o2.getId());
	};

	private static Stream<String> getNonCoreComponentNames() {
		return SetupHelper.getAvailableComponents().stream()
				.filter(def -> !def.isCoreComponent())
				.map(bean -> bean.getName());
	}
	
	private static Stream<String> getActiveNonCoreComponentNames() {
		return SetupHelper.getAvailableComponents().stream()
				.filter(def -> def.isActive())
				.filter(def -> !def.isCoreComponent())
				.map(bean -> bean.getName());
	}

	private CommandlineParameter ROOT_USERNAME = new CommandlineParameter.Builder("R", DBSchemaLoader.PARAMETERS_ENUM.ROOT_USERNAME.getName())
			.description("Database root account username used to create/remove tigase user and database")
			.build();

	private CommandlineParameter ROOT_PASSWORD = new CommandlineParameter.Builder("A", DBSchemaLoader.PARAMETERS_ENUM.ROOT_PASSWORD.getName())
			.description("Database root account password used to create/remove tigase user and database")
			.secret()
			.build();

	private CommandlineParameter CONFIG_FILE = new CommandlineParameter.Builder(null, ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY.replace("--",""))
			.defaultValue(ConfiguratorAbstract.PROPERTY_FILENAME_PROP_DEF)
			.description("Path to configuration file")
			.requireArguments(true)
			.required(true)
			.build();

	private CommandlineParameter COMPONENTS = new CommandlineParameter.Builder("C", "components").description(
			"List of enabled components identifiers (+/-)")
			.defaultValue(getActiveNonCoreComponentNames().sorted().collect(Collectors.joining(",")))
			.options(getNonCoreComponentNames().sorted().toArray(x -> new String[x]))
			.build();

	private final List<Class<?>> repositoryClasses;
	private Map<String, Object> config;

	private String rootUser;
	private String rootPass;

	public static void main(String args[]) throws IOException, ConfigReader.ConfigException {
		try {
			SchemaManager schemaManager = new SchemaManager();
			schemaManager.execute(args);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	public void execute(String args[]) throws Exception {
		String scriptName = System.getProperty("scriptName");
		ParameterParser parser = new ParameterParser(true);

		parser.setTasks(new Task[] {
				new Task.Builder().name("upgrade-schema")
						.description("Upgrade schema of databases specified in your config file - it's not possible to specify parameters")
						.additionalParameterSupplier(this::upgradeSchemaParametersSupplier)
						.function(this::upgradeSchema).build(),
				new Task.Builder().name("install-schema")
						.description("Install schema to database - it requires specifying database parameters where schema will be installed (config file will be ignored)")
						.additionalParameterSupplier(this::installSchemaParametersSupplier)
						.function(this::installSchema).build(),
				new Task.Builder().name("destroy-schema")
						.description("Destroy database and schemas (DANGEROUS)")
						.additionalParameterSupplier(this::destroySchemaParametersSupplier)
						.function(this::destroySchema).build()

		});

		Properties props = parser.parseArgs(args);
		Optional<Task> task = parser.getTask();
		if (props != null && task.isPresent()) {
			task.get().execute(props);
		} else {
			String executionCommand = null;
			if (scriptName != null) {
				executionCommand = "$ " + scriptName + " [task] [params-file.conf] [options]" + "\n\t\t" +
						"if the option defines default then <value> is optional";
			}

			System.out.println(parser.getHelp(executionCommand));
		}
	}
	                                                                                                                             
	private List<CommandlineParameter> destroySchemaParametersSupplier() {
		List<CommandlineParameter> options = new ArrayList<>();
		options.addAll(Arrays.asList(ROOT_USERNAME, ROOT_PASSWORD, CONFIG_FILE));
		options.addAll(SchemaLoader.getMainCommandlineParameters(true));
		return options;
	}

	public void destroySchema(Properties props) throws IOException, ConfigReader.ConfigException {
		fixShutdownThreadIssue();
		String type = props.getProperty(DBSchemaLoader.PARAMETERS_ENUM.DATABASE_TYPE.getName());
		Map<String, Object> config = null;
		if (type != null) {
			SchemaLoader loader = SchemaLoader.newInstance(type);
			SchemaLoader.Parameters params = loader.createParameters();
			params.setProperties(props);
			loader.init(params);
			String dbUri = loader.getDBUri();

			String[] vhosts = new String[]{DNSResolverFactory.getInstance().getDefaultHost()};
			ConfigBuilder configBuilder = SetupHelper.generateConfig(ConfigTypeEnum.DefaultMode, dbUri, false, false,
																	 Optional.empty(), Optional.empty(), Optional.empty(), vhosts,
																	 Optional.empty(), Optional.empty());

			config = configBuilder.build();
		} else {
			Optional<String> configFile = getProperty(props, CONFIG_FILE);
			try (FileReader reader = new FileReader(configFile.get())) {
				 config = new ConfigReader().read(reader);
			}
		}

		Optional<String> rootUser = getProperty(props, ROOT_USERNAME);
		Optional<String> rootPass = getProperty(props, ROOT_PASSWORD);
		
		setConfig(config);
		if (rootUser.isPresent() && rootPass.isPresent()) {
			setDbRootCredentials(rootUser.get(), rootPass.get());
		}

		Map<String, DataSourceInfo> result = getDataSources();
		log.info("found " + result.size() + " data sources to destroy...");
		Map<DataSourceInfo, List<ResultEntry>> results = destroySchemas(result.values());
		log.info("data sources  destruction finished!");
		List<String> output = prepareOutput("Data source destruction finished", results);
		TigaseRuntime.getTigaseRuntime().shutdownTigase(output.toArray(new String[output.size()]));
	}

	private List<CommandlineParameter> installSchemaParametersSupplier() {

		List<CommandlineParameter> options = new ArrayList<>();
		options.add(COMPONENTS);
		options.addAll(SchemaLoader.getMainCommandlineParameters(false));
		return options;
	}

	public void installSchema(Properties props) throws IOException, ConfigReader.ConfigException {
		String type = props.getProperty(DBSchemaLoader.PARAMETERS_ENUM.DATABASE_TYPE.getName());
		SchemaLoader loader = SchemaLoader.newInstance(type);
		SchemaLoader.Parameters params = loader.createParameters();
		params.setProperties(props);
		loader.init(params);
		String dbUri = loader.getDBUri();

		Map<String, Set<String>> changes = getProperty(props, COMPONENTS,
													   (listStr) -> Arrays.asList(listStr.split(","))).orElse(
				Collections.emptyList())
				.stream()
				.collect(Collectors.groupingBy((v) -> v.startsWith("-") ? "-" : "+", Collectors.mapping(
						(v) -> (v.startsWith("-") || v.startsWith("+")) ? v.substring(1) : v, Collectors.toSet())));

		Set<String> components = getActiveNonCoreComponentNames().collect(Collectors.toSet());

		changes.forEach((k,v) -> {
			switch (k) {
				case "+":
					components.addAll(v);
					break;
				case "-":
					components.removeAll(v);
					break;
			}
		});


		String[] vhosts = new String[]{DNSResolverFactory.getInstance().getDefaultHost()};
		ConfigBuilder configBuilder = SetupHelper.generateConfig(ConfigTypeEnum.DefaultMode, dbUri, false, false,
																 Optional.ofNullable(components), Optional.ofNullable(changes.get("+")), Optional.empty(), vhosts,
																 Optional.empty(), Optional.empty());

		Map<String, Object> config = configBuilder.build();
		List<String> output = loadSchemas(config, props, "Schema installation finished");

		output.add("");
		output.add("Example init.properties configuration file:");
		output.add("");
		try (StringWriter writer = new StringWriter()) {
			new ConfigWriter().write(writer, config);
			output.addAll(Arrays.stream(writer.toString().split("\n")).collect(Collectors.toList()));
		}
		TigaseRuntime.getTigaseRuntime().shutdownTigase(output.toArray(new String[output.size()]));
	}

	private List<CommandlineParameter> upgradeSchemaParametersSupplier() {
		return Arrays.asList(ROOT_USERNAME, ROOT_PASSWORD, CONFIG_FILE);
	}

	public void upgradeSchema(Properties props) throws IOException, ConfigReader.ConfigException {
		Optional<String> configFile = getProperty(props, CONFIG_FILE);
		try (FileReader reader = new FileReader(configFile.get())) {
			Map<String, Object> config = new ConfigReader().read(reader);
			List<String> output = loadSchemas(config, props, "Schema upgrade finished");
			TigaseRuntime.getTigaseRuntime().shutdownTigase(output.toArray(new String[output.size()]));
		}
	}

	private List<String> loadSchemas(Map<String, Object> config, Properties props, String title) throws IOException, ConfigReader.ConfigException {
		Optional<String> rootUser = getProperty(props, ROOT_USERNAME);
		Optional<String> rootPass = getProperty(props, ROOT_PASSWORD);

		setConfig(config);
		if (rootUser.isPresent() && rootPass.isPresent()) {
			setDbRootCredentials(rootUser.get(), rootPass.get());
		}

		Map<DataSourceInfo, List<SchemaInfo>> result = getDataSourcesAndSchemas();
		log.info("found " + result.size() + " data sources to upgrade...");

		log.info("begining upgrade...");
		Map<DataSourceInfo, List<ResultEntry>> results = loadSchemas();
		log.info("schema upgrade finished!");
		return prepareOutput(title, results);
	}

	private List<String> prepareOutput(String title, Map<DataSourceInfo, List<ResultEntry>> results) {
		List<String> output = new ArrayList<>(Arrays.asList("\t" + title));
		results.forEach((k,v) -> {
			output.add("");
			output.add("Data source: " + k.getName() + " with uri " + k.getResourceUri());
			v.forEach(r -> {
				output.add("\t" + r.name + "\t" + r.result);
				if (r.result != SchemaLoader.Result.ok && r.message != null) {
					String[] lines = r.message.split("\n");
					for (int i=0; i<lines.length; i++) {
						if (i == 0) {
							output.add("\t\tMessage: " + lines[0]);
						} else {
							output.add("\t\t         " + lines[i]);
						}
					}
				}
			});
		});
		return output;
	}

	public static Optional<String> getProperty(Properties props, CommandlineParameter parameter) {
		Optional<String> value = Optional.ofNullable(props.getProperty(parameter.getFullName(false).get()));
		if (!value.isPresent()) {
			return parameter.getDefaultValue();
		}
		return value;
	}

	public static <T> Optional<T> getProperty(Properties props, CommandlineParameter parameter, Function<String, T> converter) {
		Optional<String> value = getProperty(props, parameter);
		if (!value.isPresent()) {
			return Optional.empty();
		}
		T result = converter.apply(value.get());
		return Optional.ofNullable(result);
	}

	public SchemaManager() {
		repositoryClasses = ClassUtilBean.getInstance()
				.getAllClasses()
				.stream()
				.filter(clazz -> Arrays.stream(SUPPORTED_CLASSES)
						.filter(supClazz -> supClazz.isAssignableFrom(clazz))
						.findAny()
						.isPresent())
				.filter(clazz -> {
					Bean bean = clazz.getAnnotation(Bean.class);
					return bean != null && (bean.parent() != Object.class || bean.parents().length > 0);
				})
				.filter(clazz -> !DataSourceBean.class.isAssignableFrom(clazz))
				.collect(Collectors.toList());

		log.log(Level.FINE, "found following data source related classes: {0}", repositoryClasses);
	}

	public void readConfig(File file) throws IOException, ConfigReader.ConfigException {
		config = new ConfigReader().read(file);
	}

	public void readConfig(String configString) throws IOException, ConfigReader.ConfigException {
		try (StringReader reader = new StringReader(configString)) {
			readConfig(reader);
		}
	}

	public void readConfig(Reader reader) throws IOException, ConfigReader.ConfigException {
		config = new ConfigReader().read(reader);
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	public void setDbRootCredentials(String user, String pass) {
		rootUser = user;
		rootPass = pass;
	}

	public Map<DataSourceInfo, List<SchemaInfo>> getDataSourcesAndSchemas() {
		Kernel kernel = prepareKernel();
		List<BeanConfig> repoBeans = getRepositoryBeans(kernel);
		List<RepoInfo> repositories = getRepositories(kernel, repoBeans);
		Map<DataSourceInfo, List<RepoInfo>> repositoriesByDataSource = groupRepositoriesByDataSource(repositories);

		return collectSchemasByDataSource(repositoriesByDataSource);
	}

//	public String getSchemaFileName(SchemaInfo schemaInfo, DataSource ds) {
//		return getSchemaFileName(schemaInfo, ds.getResourceUri());
//	}

	public Map<DataSourceInfo, List<ResultEntry>> destroySchemas(Collection<DataSourceInfo> dataSources) {
		return dataSources.stream().map(e -> new Pair<>(e, destroySchemas(e))).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	public List<ResultEntry> destroySchemas(DataSource ds) {
		return executeWithSchemaLoader(ds, (schemaLoader, handler) -> {
			List<ResultEntry> results = new ArrayList<>();
			log.log(Level.FINEST, "removing database for data source " + ds);
			results.add(new ResultEntry("Destroying data source", schemaLoader.destroyDataSource(), handler));
			return results;
		});
	}

	public Map<DataSourceInfo, List<ResultEntry>> loadSchemas() {
		Map<DataSourceInfo, List<SchemaInfo>> dataSourceSchemas = getDataSourcesAndSchemas();
		return dataSourceSchemas.entrySet()
				.stream()
				.map(e -> new Pair<DataSourceInfo, List<ResultEntry>>(e.getKey(), loadSchemas(e.getKey(), e.getValue())))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	public Map<DataSourceInfo, List<ResultEntry>> loadSchemas(String uri) {
		Map<DataSourceInfo, List<SchemaInfo>> dataSourceSchemas = getDataSourcesAndSchemas();
		return dataSourceSchemas.entrySet()
				.stream()
				.filter(e -> uri.equals(e.getKey().getResourceUri()))
				.map(e -> new Pair<DataSourceInfo, List<ResultEntry>>(e.getKey(), loadSchemas(e.getKey(), e.getValue())))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	public List<ResultEntry> loadSchemas(DataSource ds, List<SchemaInfo> schemas) {
		if (!schemas.stream().filter(SchemaInfo::isValid).findAny().isPresent()) {
			log.log(Level.FINER, "no known schemas for data source " + ds + ", skipping schema loading...");
			return Collections.EMPTY_LIST;
		}

		return executeWithSchemaLoader(ds, (schemaLoader, handler) -> {
			List<ResultEntry> results = new ArrayList<>();
			results.add(new ResultEntry("Checking if database exists", schemaLoader.validateDBExists(), handler));
			log.log(Level.FINER, "loading schemas for data source " + ds);
			schemas.sort(SCHEMA_INFO_COMPARATOR);
			results.addAll(schemas.stream().filter(schema -> schema.isValid()).map(schema -> {
				log.log(Level.FINER, "loading schema with id ='" + schema + "'");
				return new ResultEntry(
						"Loading schema: " + schema.getName() + ", version: " + schema.getVersion(),
						schemaLoader.loadSchema(schema.getId(), schema.getVersion()), handler);
			}).collect(Collectors.toList()));

			if (schemas.stream().filter(schema -> Schema.SERVER_SCHEMA_ID.equals(schema.getId())).findAny().isPresent()) {
				results.add(new ResultEntry("Adding XMPP admin accounts", schemaLoader.addXmppAdminAccount(), handler));
			}

			results.add(new ResultEntry("Post installation action", schemaLoader.postInstallation(), handler));
			return results;
		});
	}

	private List<ResultEntry> executeWithSchemaLoader(DataSource ds, SchemaLoaderExecutor function) {
		SchemaLoader schemaLoader = SchemaLoader.newInstanceForURI(ds.getResourceUri());
		List<ResultEntry> results = new ArrayList<>();

		Logger logger = Logger.getLogger(schemaLoader.getClass().getCanonicalName());
		SchemaManagerLogHandler handler = Arrays.stream(logger.getHandlers())
				.filter(h -> h instanceof SchemaManagerLogHandler)
				.map(h -> (SchemaManagerLogHandler) h)
				.findAny()
				.orElseGet(() -> {
					SchemaManagerLogHandler handler1 = new SchemaManagerLogHandler();
					logger.addHandler(handler1);
					return handler1;
				});
		handler.setLevel(Level.FINEST);
		logger.setLevel(Level.FINEST);

		SchemaLoader.Parameters params = schemaLoader.createParameters();
		params.parseUri(ds.getResourceUri());
		if (rootUser != null || rootPass != null) {
			params.setDbRootCredentials(rootUser, rootPass);
		}
		schemaLoader.init(params);

		results.add(new ResultEntry("Checking connection to database", schemaLoader.validateDBConnection(), handler));

		results.addAll(function.execute(schemaLoader, handler));

		schemaLoader.shutdown();
		return results;
	}

	private Map<String, DataSourceInfo> getDataSources() {
		Map<String, DataSourceInfo> dataSources = ((Map<String, Object>) config.get("dataSource")).values()
				.stream()
				.filter(v -> v instanceof AbstractBeanConfigurator.BeanDefinition)
				.map(v -> (AbstractBeanConfigurator.BeanDefinition) v)
				.filter(def -> def.isActive())
				.map(def -> new DataSourceInfo(def.getBeanName(),
											   (String) def.getOrDefault("uri", def.get("repo-uri"))))
				.collect(Collectors.toMap(DataSourceInfo::getName, Function.identity()));
		return dataSources;
	}

	private List<RepoInfo> getRepositories(Kernel kernel, List<BeanConfig> repoBeans) {
		DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		Map<String, DataSourceInfo> dataSources = getDataSources();
		return repoBeans.stream().flatMap(bc -> {
			try {
				if (SDRepositoryBean.class.isAssignableFrom(bc.getClazz())) {
					String dataSourceName = getDataSourceNameOr(configurator, bc, "default");
					DataSourceInfo dataSource = dataSources.get(dataSourceName);
					Class<?> implementation = getRepositoryImplementation(configurator, dataSource, bc, null);
					return Stream.of(new RepoInfo(bc, dataSource, implementation));
				} else {
					return bc.getKernel()
							.getDependencyManager()
							.getBeanConfigs()
							.stream()
							.filter(bc1 -> !Kernel.class.isAssignableFrom(bc1.getClazz()))
							.filter(bc1 -> !Kernel.DelegatedBeanConfig.class.isAssignableFrom(bc1.getClass()))
							.map(bc1 -> {
								try {
									String dataSourceName = getDataSourceNameOr(configurator, bc1, bc1.getBeanName());
									DataSourceInfo dataSource = dataSources.get(dataSourceName);
									Class<?> implementation = getRepositoryImplementation(configurator, dataSource, bc1,
																						  bc);
									return new RepoInfo(bc1, dataSource, implementation);
								} catch (Exception ex) {
									ex.printStackTrace();
									return null;
								}
							});
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				return Stream.empty();
			}
		}).filter(repo -> repo != null).collect(Collectors.toList());
	}

	private Map<DataSourceInfo, List<SchemaInfo>> collectSchemasByDataSource(Map<DataSourceInfo, List<RepoInfo>> repositoriesByDataSource) {
		Map<DataSourceInfo, List<SchemaInfo>> dataSourceSchemas = new HashMap<>();
		for (Map.Entry<DataSourceInfo,List<RepoInfo>> entry : repositoriesByDataSource.entrySet()) {

			List<SchemaInfo> schemas = entry.getValue()
					.stream()
					.collect(Collectors.groupingBy(repoInfo -> getSchemaId(repoInfo), Collectors.toList()))
					.entrySet()
					.stream()
					.map(e -> new SchemaInfo(e.getValue().iterator().next().getImplementation().getAnnotation(Repository.SchemaId.class), e.getValue()))
					.collect(Collectors.toList());

			dataSourceSchemas.put(entry.getKey(), schemas);
		}

		return dataSourceSchemas;
	}

	private Map<DataSourceInfo, List<RepoInfo>> groupRepositoriesByDataSource(List<RepoInfo> repos) {
		return repos.stream()
				.collect(Collectors.toMap(repo -> repo.getDataSource(),
										  repo -> new ArrayList(Arrays.asList(repo)),
										  (prev, repo) -> {
											  prev.addAll(repo);
											  return prev;
										  }));
	}

	private String getSchemaId(RepoInfo repoInfo) {
		Repository.SchemaId schemaId = repoInfo.getImplementation().getAnnotation(Repository.SchemaId.class);
		return schemaId == null ? "<unknown>" : schemaId.id();
	}

	private String getDataSourceNameOr(DSLBeanConfigurator configurator, BeanConfig bc, String defValue) {
		Map<String, Object> cfg = configurator.getConfiguration(bc);
		return (String) cfg.getOrDefault("dataSourceName", cfg.getOrDefault("data-source", defValue));
	}

	private Class<?> getRepositoryImplementation(DSLBeanConfigurator configurator, DataSourceInfo dataSource,
												 BeanConfig beanConfig, BeanConfig mdRepoBeanConfig)
			throws ClassNotFoundException, DBInitException, IllegalAccessException, InstantiationException,
				   NoSuchMethodException, InvocationTargetException {
		Map<String, Object> cfg = configurator.getConfiguration(beanConfig);
		String cls = (String) cfg.getOrDefault("cls", cfg.get("repo-cls"));
		if (cls != null) {
			return ModulesManagerImpl.getInstance().forName(cls);
		}

		Object bean = beanConfig.getClazz().newInstance();
		if (bean instanceof MDPoolConfigBean) {
			Method m = MDPoolConfigBean.class.getDeclaredMethod("getRepositoryIfc");
			m.setAccessible(true);
			return DataSourceHelper.getDefaultClass((Class<?>) m.invoke(bean), dataSource.getResourceUri());
		}
		if (bean instanceof SDRepositoryBean) {
			Method m = SDRepositoryBean.class.getDeclaredMethod("findClassForDataSource", DataSource.class);
			m.setAccessible(true);
			return (Class<?>) m.invoke(bean, dataSource);
		}
		if (mdRepoBeanConfig != null) {
			Object mdRepoBean = mdRepoBeanConfig.getClazz().newInstance();
			if (mdRepoBean instanceof MDRepositoryBean) {
				Method m = MDRepositoryBean.class.getDeclaredMethod("findClassForDataSource", DataSource.class);
				m.setAccessible(true);
				return (Class<?>) m.invoke(mdRepoBean, dataSource);
			}
		}
		throw new RuntimeException("Unknown repository!");
	}

	private Kernel prepareKernel() {
		Kernel kernel = new Kernel("root");
		try {
			if (XMPPServer.isOSGi()) {
				kernel.registerBean("classUtilBean")
						.asInstance(Class.forName("tigase.osgi.util.ClassUtilBean").newInstance())
						.exportable()
						.exec();
			} else {
				kernel.registerBean("classUtilBean")
						.asInstance(Class.forName("tigase.util.ClassUtilBean").newInstance())
						.exportable()
						.exec();
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		// register default types converter and properties bean configurator
		kernel.registerBean(DefaultTypesConverter.class).exportable().exec();
		kernel.registerBean(DSLBeanConfiguratorWithBackwardCompatibility.class).exportable().exec();
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();

		DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		configurator.setProperties(config);
		ModulesManagerImpl.getInstance().setBeanConfigurator(configurator);

		kernel.registerBean("beanSelector").asInstance(new ServerBeanSelector()).exportable().exec();

		return kernel;
	}

	private List<BeanConfig> getRepositoryBeans(Kernel kernel) {
		DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		configurator.registerBeans(null, null, config);

		List<BeanConfig> repoBeans = crawlKernel(repositoryClasses, kernel, configurator, config);
		fixShutdownThreadIssue();
		return repoBeans;
	}

	private void fixShutdownThreadIssue() {
		MonitorRuntime.getMonitorRuntime();
		try {
			Field f = MonitorRuntime.class.getDeclaredField("mainShutdownThread");
			f.setAccessible(true);
			MonitorRuntime monitorRuntime = MonitorRuntime.getMonitorRuntime();
			Runtime.getRuntime().removeShutdownHook((Thread) f.get(monitorRuntime));
		} catch (NoSuchFieldException | IllegalAccessException ex) {
			log.log(Level.FINEST, "There was an error with unregistration of shutdown hook", ex);
		}
	}

	private List<BeanConfig> crawlKernel(List<Class<?>> repositoryClasses, Kernel kernel,
										 DSLBeanConfigurator configurator, Map<String, Object> config) {
		List<BeanConfig> results = new ArrayList<>();
		kernel.getDependencyManager()
				.getBeanConfigs()
				.stream()
				.filter(bc -> bc.getState() == BeanConfig.State.registered)
				.filter(bc -> !Kernel.DelegatedBeanConfig.class.isAssignableFrom(bc.getClass()))
				.forEach(bc -> {
					try {
						Object bean = bc.getClazz().newInstance();
						if (RegistrarBean.class.isAssignableFrom(bc.getClazz())) {
							RegistrarKernel k = new RegistrarKernel();
							k.setName(bc.getBeanName());
							bc.getKernel().registerBean(bc.getBeanName() + "#KERNEL").asInstance(k).exec();
							Method m = bc.getClass().getDeclaredMethod("setKernel", Kernel.class);
							m.setAccessible(true);
							m.invoke(bc, k);

							Kernel parent = bc.getKernel().getParent();
							// without this line setBeanActive() fails
							//parent.ln(beanConfig.getBeanName(), beanConfig.getKernel(), beanConfig.getBeanName());
							parent.ln(bc.getBeanName(), bc.getKernel(), "service");

							((RegistrarBean) bean).register(bc.getKernel());
							Map<String,Object> cfg = (Map<String, Object>) config.getOrDefault(bc.getBeanName(), new HashMap<>());
							configurator.registerBeans(bc, bean, cfg);
							results.addAll(crawlKernel(repositoryClasses, bc.getKernel(), configurator, cfg));
						}

						if (repositoryClasses.stream()
								.filter(repoClazz -> repoClazz.isAssignableFrom(bc.getClazz()))
								.findAny()
								.isPresent()) {
							results.add(bc);
						}

					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (StackOverflowError ex) {
						ex.printStackTrace();
						Kernel k = bc.getKernel();
						List<String> list = new ArrayList<>();
						do {
							list.add(k.getName());
						} while ((k = k.getParent()) != null);
						System.out.println("exception in path " + list);
					}
				});
		return results;
	}

	public static class DataSourceInfo
			implements DataSource {

		private final String name;
		private final String uri;

		private DataSourceInfo(String name, String uri) {
			this.name = name;
			this.uri = uri;
		}

		public String getName() {
			return name;
		}

		@Override
		public String getResourceUri() {
			return uri;
		}

		@Override
		public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
			// nothing to do
		}

		@Override
		public String toString() {
			return name + "[uri=" + uri + "]";
		}
	}

	public static class RepoInfo {

		private final BeanConfig beanConfig;
		private final DataSourceInfo dataSource;
		private final Class<?> implementation;

		public RepoInfo(BeanConfig beanConfig, DataSourceInfo dataSource, Class<?> implementation) {
			this.beanConfig = beanConfig;
			this.dataSource = dataSource;
			this.implementation = implementation;
		}

		public DataSourceInfo getDataSource() {
			return dataSource;
		}

		public Class<?> getImplementation() {
			return implementation;
		}

		@Override
		public String toString() {
			return beanConfig.getBeanName() + "[dataSource=" + dataSource.getName() + ", class=" + implementation + "]";
		}
	}

	public static class SchemaInfo {

		private final Repository.SchemaId schema;
		private final RepoInfo[] repositories;

		public SchemaInfo(Repository.SchemaId schema, List<RepoInfo> repositories) {
			this.schema = schema;
			this.repositories = repositories.toArray(new RepoInfo[repositories.size()]);
		}

		public String getId() {
			return (schema == null ? "<unknown>" : schema.id());
		}

		public String getName() {
			return (schema != null ? schema.name() : "");
		}

		public RepoInfo[] getRepositories() {
			return repositories;
		}

		public String getVersion() {
			try {
				Map<String, String> map = Arrays.asList(repositories)
						.stream()
						.map(repo -> repo.getImplementation().getPackage())
						.collect(Collectors.toMap(p -> p.getImplementationTitle(),
												  p -> Optional.ofNullable(p.getImplementationVersion())
														  .orElse("0.0.0")
														  .split("-")[0], (v1, v2) -> v1));

				if (map.size() > 1) {
					throw new RuntimeException("Could not detect schema version = " + map);
				}

				return map.values().iterator().next();
			} catch (Exception ex) {
				return null;
			}
		}
		
		public boolean isValid() {
			return schema != null && getVersion() != null;
		}                          

		@Override
		public String toString() {
			return "SchemaInfo[id=" + (schema == null ? "<unknown>" : schema.id()) + ", repositories=" + Arrays.asList(repositories) + "]";
		}
	}

	public static class ResultEntry {

		public final String name;
		public final SchemaLoader.Result result;
		public final String message;

		private ResultEntry(String name, SchemaLoader.Result result, SchemaManagerLogHandler logHandler) {
			this.name = name;
			this.result = result;
			LogRecord rec;
			StringBuilder sb = null;
			while ((rec = logHandler.poll()) != null) {
				if (rec.getLevel().intValue() <= Level.FINE.intValue()) {
					continue;
				}
				if (rec.getMessage() == null) {
					continue;
				}
				if (sb == null) {
					sb = new StringBuilder();
				} else {
					sb.append("\n");
				}
				sb.append(String.format(rec.getMessage(), rec.getParameters()));
			}
			this.message = sb == null ? null : sb.toString();
		}

	}

	public static class Pair<K,V> {

		private final K key;
		private final V value;

		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}
	}

	public interface SchemaLoaderExecutor {
		List<ResultEntry> execute(SchemaLoader schemaLoader, SchemaManagerLogHandler handler);
	}
}
