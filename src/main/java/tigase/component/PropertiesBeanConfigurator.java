/*
 * PropertiesBeanConfigurator.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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

package tigase.component;

import tigase.kernel.BeanUtils;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.*;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = BeanConfigurator.DEFAULT_CONFIGURATOR_NAME, active = true)
public class PropertiesBeanConfigurator
		extends AbstractBeanConfigurator {

	private static final Logger log = Logger.getLogger(PropertiesBeanConfigurator.class.getCanonicalName());

	private Map<String, Object> props;

	public Collection<ConfigEntry> getAllConfigOptions() {
		return getCurrentConfigurations(false);
	}

	@Override
	protected Map<String, BeanDefinition> getBeanDefinitions(Map<String, Object> values) {
		Map<String, BeanDefinition> beanPropConfigMap = super.getBeanDefinitions(values);

		List<String> keys = new ArrayList<>(values.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			String[] keyParts = key.split("/");
			if (keyParts.length != 2) {
				continue;
			}

			String beanName = keyParts[0];
			String action = keyParts[1];
			Object value = values.get(key);

			BeanDefinition cfg = beanPropConfigMap.get(beanName);
			switch (action) {
				case "active":
				case "class":
					if (cfg == null) {
						cfg = new BeanDefinition();
						cfg.setBeanName(beanName);
						beanPropConfigMap.put(beanName, cfg);
					}
					break;
				default:
					if (kernel.isBeanClassRegistered(beanName) && cfg == null) {
						cfg = new BeanDefinition();
						cfg.setBeanName(beanName);
						beanPropConfigMap.put(beanName, cfg);
					}
					break;
			}
			switch (action) {
				case "active":
					cfg.setActive(Boolean.parseBoolean(value.toString()));
					break;
				case "class":
					cfg.setClazzName(value.toString());
					break;
				default:
					break;
			}
		}

		return beanPropConfigMap;
	}

	private HashMap<String, Object> getBeanProps(BeanConfig beanConfig) {
		HashMap<String, Object> result = new HashMap<>();

		Map<String, String> configAliasses = getConfigAliasses(beanConfig);

		// this map needs to be filled with name of fields or field aliases
		// which implements Map
		Set<String> mapFields = new HashSet<>();
		Set<String> aliases = new HashSet<>();
		Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
		for (Field field : fields) {
			ConfigField cf = field.getAnnotation(ConfigField.class);
			if (cf != null) {
				if (Map.class.isAssignableFrom(field.getType())) {
					mapFields.add(field.getName());
				}
				String alias = configAliasses.get(field.getName());
				if (alias == null) {
					alias = cf.alias();
				}
				if (!alias.isEmpty()) {
					aliases.add(alias);
					if (Map.class.isAssignableFrom(field.getType())) {
						mapFields.add(alias);
					}
				}
			}
		}

		if (props != null) {
			List<String> path = new ArrayList<>(getBeanConfigPath(beanConfig));

			for (int i = 0; i <= path.size(); i++) {
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < i; j++) {
					if (sb.length() != 0) {
						sb.append("/");
					}
					sb.append(path.get(j));
				}
				String prefix = sb.toString();
				for (Map.Entry<String, Object> e : props.entrySet()) {
					if (prefix.isEmpty() || e.getKey().startsWith(prefix + "/")) {
						String key = prefix.isEmpty() ? e.getKey() : e.getKey().substring(prefix.length() + 1);
						int idx = key.indexOf("/");
						// if there is next / char and there is field or alias implementing Map for prefix of this key
						// then we need to gather all key with this prefix and create map of values
						if (idx > 0) {
							String fname = key.substring(0, idx);
							if (mapFields.contains(fname)) {
								Map<String, Object> vals = (Map<String, Object>) result.get(fname);
								if (vals == null) {
									vals = new HashMap<>();
									result.put(fname, vals);
								}
								vals.put(key.substring(idx + 1), e.getValue());
								continue;
							}
						}
						if (i == path.size() || aliases.contains(key)) {
							result.put(key, e.getValue());
						}
					}
				}
			}
		}

		result.put("name", beanConfig.getBeanName());

		return result;
	}

	protected boolean hasDirectConfiguration(BeanConfig beanConfig) {
		ArrayDeque<String> path = getBeanConfigPath(beanConfig);
		StringBuilder sb = new StringBuilder();

		String name;
		while ((name = path.poll()) != null) {
			sb.append(name);
			sb.append('/');
		}

		String prefix = sb.toString();

		for (Map.Entry<String, Object> e : props.entrySet()) {
			if (e.getKey().startsWith(prefix)) {
				if (e.getKey().substring(prefix.length()).indexOf('/') == -1) {
					return true;
				}
			}
		}

		return false;
	}

	protected Map<String, String> getConfigAliasses(BeanConfig beanConfig) {
		Map<String, String> configAliasses = new HashMap<>();
		Class<?> cls = beanConfig.getClass();
		do {
			ConfigAliases ca = cls.getAnnotation(ConfigAliases.class);
			if (ca != null) {
				for (ConfigAlias a : ca.value()) {
					configAliasses.put(a.field(), a.alias());
				}
			} else {
				break;
			}
		} while ((cls = cls.getSuperclass()) != null);
		return configAliasses;
	}

	@Override
	protected Map<String, Object> getConfiguration(BeanConfig beanConfig) {
		final HashMap<String, Object> valuesToSet = new HashMap<>();

		resolveAliases(beanConfig, props, valuesToSet);

		// Preparing set of properties based on given properties set
		HashMap<String, Object> beanProps = getBeanProps(beanConfig);
		resolveAliases(beanConfig, beanProps, valuesToSet);
		for (Map.Entry<String, Object> e : beanProps.entrySet()) {
			final String property = e.getKey();
			final Object value = e.getValue();

			valuesToSet.put(property, value);
		}

		return valuesToSet;
	}

	public Collection<ConfigEntry> getCurrentConfigurations() {
		return getCurrentConfigurations(true);
	}

	private Collection<ConfigEntry> getCurrentConfigurations(boolean forceInit) {
		HashSet<ConfigEntry> result = new HashSet<>();

		for (BeanConfig bc : kernel.getDependencyManager().getBeanConfigs()) {

			final Object bean;
			if (forceInit || bc.getState() == BeanConfig.State.initialized) {
				bean = kernel.getInstance(bc.getBeanName());
			} else {
				bean = null;
			}
			final Class<?> cl = bc.getClazz();
			Field[] fields = DependencyManager.getAllFields(cl);
			for (Field field : fields) {
				final ConfigField cf = field.getAnnotation(ConfigField.class);
				if (cf != null) {
					String key = bc.getBeanName() + "/" + field.getName();
					try {
						Object currentValue = bean == null ? null : BeanUtils.getValue(bean, field);

						ConfigEntry entry = new ConfigEntry();
						entry.property = key;
						entry.value = currentValue;
						entry.details = cf;

						result.add(entry);
					} catch (IllegalAccessException | InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return result;
	}

	public Map<String, Object> getProperties() {
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		this.props = props;
	}

	protected void resolveAliases(BeanConfig beanConfig, Map<String, Object> props, Map<String, Object> valuesToSet) {
		// Preparing set of properties based on @ConfigField annotation and
		// aliases
		Map<String, String> configAliasses = getConfigAliasses(beanConfig);
		Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
		for (Field field : fields) {
			final ConfigField cf = field.getAnnotation(ConfigField.class);
			if (cf != null && !cf.alias().isEmpty() && props.containsKey(cf.alias()) &&
					(this.props != props || cf.allowAliasFromParent())) {
				final Object value = props.get(cf.alias());

				if (props.containsKey(field)) {
					if (log.isLoggable(Level.CONFIG)) {
						log.config("Alias '" + cf.alias() + "' for property " + beanConfig.getBeanName() + "." +
										   field.getName() +
										   " will not be used, because there is configuration for this property already.");
					}
					continue;
				}
				if (log.isLoggable(Level.CONFIG)) {
					log.config("Using alias '" + cf.alias() + "' for property " + beanConfig.getBeanName() + "." +
									   field.getName());
				}

				valuesToSet.put(field.getName(), value);
			}
			if (cf != null && configAliasses.containsKey(field.getName())) {
				String alias = configAliasses.get(field.getName());
				final Object value = props.get(alias);
				if (value != null) {
					valuesToSet.put(field.getName(), value);
				}
			}
		}
	}

	public static class ConfigEntry {

		private ConfigField details;
		private String property;
		private Object value;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ConfigEntry that = (ConfigEntry) o;

			return property.equals(that.property);

		}

		public ConfigField getDetails() {
			return details;
		}

		public String getProperty() {
			return property;
		}

		public Object getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			return property.hashCode();
		}
	}

}
