/*
 * @(#)XMPPServer.java   2010.01.15 at 08:51:06 PST
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.conf.ConfigReader;
import tigase.conf.ConfiguratorAbstract;
import tigase.kernel.KernelException;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.beans.selector.ServerBeanSelector;
import tigase.kernel.core.BeanConfig;
import tigase.sys.TigaseRuntime;
import tigase.util.ClassUtil;
import tigase.xml.XMLUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

//~--- classes ----------------------------------------------------------------
/**
 * Describe class XMPPServer here.
 *
 *
 * Created: Wed Nov 23 07:04:18 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public final class XMPPServer {

	@SuppressWarnings("PMD")
	/** property allowing setting up configurator implementation of
	 * {@link ConfiguratorAbstract} used in Tigase.
	 */
	public static final String CONFIGURATOR_PROP_KEY = "tigase-configurator";

	/** default configurator implementation of
	 * {@link ConfiguratorAbstract} used in Tigase, which is
	 * tigase.conf.Configurator. */
	private static final String DEF_CONFIGURATOR = "tigase.conf.Configurator";
	public static final String HARDENED_MODE_KEY = "hardened-mode";

	public static boolean isHardenedModeEnabled() {
		return System.getProperty( XMPPServer.HARDENED_MODE_KEY ) == null ? false
					 : Boolean.getBoolean( XMPPServer.HARDENED_MODE_KEY );
	}

	/** Field description */
	public static final String NAME = "Tigase";
	private static String serverName = "message-router";

	private static Bootstrap bootstrap;
	private static boolean inOSGi = false;

	//~--- constructors ---------------------------------------------------------
	/**
	 * Creates a new <code>XMPPServer</code> instance.
	 */
	private XMPPServer() {
	}

	//~--- get methods ----------------------------------------------------------
	/**
	 * Method description
	 *
	 *
	 *
	 */
	public static String getImplementationVersion() {
		String version = ComponentInfo.getImplementationVersion( XMPPServer.class );
		return ( version.isEmpty() ? "0.0.0-0" : version );
	}

	//~--- methods --------------------------------------------------------------
	/**
	 * Returns help regarding command line parameters
	 */
	public static String help() {
		return "\n" + "Parameters:\n"
					 + " -h               this help message\n"
					 + " -v               prints server version info\n"
					 + " -n server-name    sets server name\n";
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	@SuppressWarnings("PMD")
	public static void main( final String[] args ) {

		parseParams( args );

		System.out.println( ( new ComponentInfo( XMLUtils.class ) ).toString() );
		System.out.println( ( new ComponentInfo( ClassUtil.class ) ).toString() );
		System.out.println( ( new ComponentInfo( XMPPServer.class ) ).toString() );
		start( args );
	}

	public static void start( String[] args ) {
		Thread.setDefaultUncaughtExceptionHandler( new ThreadExceptionHandler() );

		if ( !isOSGi() ){
			String initial_config
						 = "tigase.level=ALL\n" + "tigase.xml.level=INFO\n"
							 + "handlers=java.util.logging.ConsoleHandler\n"
							 + "java.util.logging.ConsoleHandler.level=ALL\n"
							 + "java.util.logging.ConsoleHandler.formatter=tigase.util.LogFormatter\n";

			ConfiguratorAbstract.loadLogManagerConfig( initial_config );
		}

		try {
			bootstrap = new Bootstrap();
			bootstrap.init(args);
			bootstrap.start();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS");
			if (ServerBeanSelector.getConfigType(bootstrap.getKernel()) == ConfigTypeEnum.SetupMode) {
				System.out.println("== " + sdf.format(new Date()) +
										   " Please setup server at http://localhost:8080/\n");
			} else {
				System.out.println("== " + sdf.format(new Date()) +
										   " Server finished starting up and (if there wasn't any error) is ready to use\n");
			}
		} catch ( ConfigReader.UnsupportedOperationException e ) {
			TigaseRuntime.getTigaseRuntime().shutdownTigase(new String[] {
					"ERROR! Terminating the server process.",
					e.getMessage() + " at line " + e.getLine() + " position " + e.getPosition(),
					"Line: " + e.getLineContent(),
					"Please fix the problem and start the server again."
			});
		} catch ( ConfigReader.ConfigException e ) {
			TigaseRuntime.getTigaseRuntime().shutdownTigase(new String[] {
					"ERROR! Terminating the server process.",
					"Issue with configuration file: " + e,
					"Please fix the problem and start the server again."
			});
		} catch ( Exception e ) {
			TigaseRuntime.getTigaseRuntime().shutdownTigase(new String[] {
					"ERROR! Terminating the server process.",
					"Problem initializing the server: " + e,
					"Please fix the problem and start the server again."
			});
		}
	}

	public static void setOSGi( boolean val ) {
		inOSGi = val;
	}

	public static boolean isOSGi() {
		return inOSGi;
	}

	public static void stop() {
		( (AbstractMessageReceiver) bootstrap.getInstance(MessageRouterIfc.class) ).stop();
	}

	/**
	 * Allows obtaining {@link ConfiguratorAbstract} implementation
	 * used by Tigase to handle all configuration of the server.
	 *
	 * @return implementation of {@link ConfiguratorAbstract}
	 *         interface.
	 */
//	@Deprecated
//	public static ConfiguratorAbstract getConfigurator() {
//		return config;
//	}

	public static <T> T getComponent(String name) {
		try {
			return bootstrap.getInstance(name);
		} catch (KernelException ex) {
			Logger.getLogger(XMPPServer.class.getCanonicalName())
					.log(Level.FINEST, "failed to retrieve instance of " + name, ex);
			return null;
		}
	}

	public static <T> T getComponent(Class<T> clazz) {
		try {
			return bootstrap.getInstance(clazz);
		} catch (KernelException ex) {
			Logger.getLogger(XMPPServer.class.getCanonicalName())
					.log(Level.FINEST, "failed to retrieve instance of " + clazz, ex);
			return null;
		}
	}

	public static <T> Stream<T> getComponents(Class<T> clazz) {
		return bootstrap.getKernel()
				.getDependencyManager()
				.getBeanConfigs()
				.stream()
				.filter(bc -> clazz.isAssignableFrom(bc.getClazz()) && bc.getState() == BeanConfig.State.initialized)
				.map(bc -> (T) bootstrap.getInstance(bc.getBeanName()));
	}

	/**
	 * Method description
	 *
	 *
	 * @param args
	 */
	@SuppressWarnings("PMD")
	public static void parseParams( final String[] args ) {
		if ( ( args != null ) && ( args.length > 0 ) ){
			for ( int i = 0 ; i < args.length ; i++ ) {
				if ( args[i].equals( "-h" ) ){
					System.out.print( help() );
					System.exit( 0 );
				}      // end of if (args[i].equals("-h"))

				if ( args[i].equals( "-v" ) ){
					System.out.print( version() );
					System.exit( 0 );
				}      // end of if (args[i].equals("-h"))

				if ( args[i].equals( "-n" ) ){
					if ( i + 1 == args.length ){
						System.out.print( help() );
						System.exit( 1 );
					} // end of if (i+1 == args.length)
					else {
						serverName = args[++i];
					}    // end of else
				}      // end of if (args[i].equals("-h"))

			}        // end of for (int i = 0; i < args.length; i++)
		}
	}

	/**
	 * Method description
	 *
	 *
	 *
	 */
	public static String version() {
		return "\n" + "-- \n" + NAME + " XMPP Server, version: " + getImplementationVersion()
					 + "\n" + "Author:  Artur Hefczyc <artur.hefczyc@tigase.org>\n" + "-- \n";
	}
}    // XMPPServer

//~ Formatted in Sun Code Convention on 2010.01.15 at 08:51:06 PST


//~ Formatted by Jindent --- http://www.jindent.com
