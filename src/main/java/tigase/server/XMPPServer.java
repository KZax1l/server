package tigase.server;

import tigase.conf.Configurator;

public class XMPPServer {
    static String config_file = "tigase-config.xml";
    static boolean debug = false;
    static boolean monit = false;

    /**
     * Creates a new <code>XMPPServer</code> instance.
     */
    protected XMPPServer() {
    }

    public static String help() {
        return "\n"
                + "Parameters:\n"
                + " -h                this help message\n"
                + " -v                prints server version info\n"
                + " -c file           location of configuration file\n"
                + " -d [true|false]   turn on|off debug mode\n"
                + " -m                turn on server monitor\n"
                ;
    }

    public static String version() {
        return "\n"
                + "-- \n"
                + "Tigase XMPP Server, version: "
                + XMPPServer.class.getPackage().getImplementationVersion() + "\n"
                + "Author:	Artur Hefczyc <artur.hefczyc@gmail.com>\n"
                + "-- \n"
                ;
    }

    public static void parseParams(final String[] args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-h")) {
                    System.out.print(help());
                    System.exit(0);
                } // end of if (args[i].equals("-h"))
                if (args[i].equals("-v")) {
                    System.out.print(version());
                    System.exit(0);
                } // end of if (args[i].equals("-h"))
                if (args[i].equals("-c")) {
                    if (i + 1 == args.length) {
                        System.out.print(help());
                        System.exit(1);
                    } // end of if (i+1 == args.length)
                    else {
                        config_file = args[++i];
                    } // end of else
                } // end of if (args[i].equals("-h"))
                if (args[i].equals("-d")) {
                    if (i + 1 == args.length) {
                        debug = true;
                    } // end of if (i+1 == args.length)
                    else {
                        ++i;
                        debug = args[i].charAt(0) != '-' &&
                                (args[i].equals("true") || args[i].equals("yes"));
                    } // end of else
                } // end of if (args[i].equals("-d"))
                if (args[i].equals("-m")) {
                    monit = true;
                } // end of if (args[i].equals("-m"))
            } // end of for (int i = 0; i < args.length; i++)
        }
    }

    /**
     * Describe <code>main</code> method here.
     *
     * @param args a <code>String[]</code> value
     */
    public static void main(final String[] args) {
        parseParams(args);
        Configurator config = new Configurator(config_file);
    }
}