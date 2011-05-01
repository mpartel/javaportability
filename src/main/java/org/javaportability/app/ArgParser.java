package org.javaportability.app;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

public class ArgParser {

    private Settings settings;
    private ConfigFileLoader configFileLoader;
    
    public ArgParser(Settings settings, ConfigFileLoader configFileLoader) {
        this.settings = settings;
        this.configFileLoader = configFileLoader;
    }
    
    public void parseArgs(String[] args) {
        Impl impl = new Impl(args);
        impl.processArgs();
        impl.checkUsage();
    }
    
    public String getUsage() {
        return
            "Usage: java -jar javaportability.jar verify [options] target [target...]\n" +
            "\n" +
            "\n" +
            "Targets can be class names or methods denoted \"pkg.Class::method\"\n" +
            "\n" +
            "Options:\n" +
            "  -p, --path <class:path:components>  Class path to search classes from.\n" +
            "                                      Omit to use the JVM's classpath.\n" +
            "  -c, --config <configfile>           Load config file\n" +
            "  -h, --help                          This help message.\n" +
            "  -v, --verbose                       Print a little more.\n" +
            "      --debug                         Print detailed debug messages.\n" +
            "\n" +
            "Config files have one directive per line. Examples:\n" +
            "  # Comment\n" +
            "  // Comment\n" +
            "  ignore java.util.Hash*\n" +
            "  safe my.*.SafeClass\n" +
            "  unsafe my.*.UnsafeClass::method\n" +
            "  allowfp my.*.SafeFloatMath::mySafe*Method\n" +
            "\n";
    }
    
    private class Impl {
        private String command;
        private LinkedList<String> remainingArgs;
        
        private Impl(String[] args) {
            remainingArgs = new LinkedList<String>(Arrays.asList(args));
        }
        
        private void processArgs() {
            if (haveHelpArg()) {
                settings.help = true;
                return;
            }
            
            requireCommand();
            
            while (!remainingArgs.isEmpty()) {
                String arg = remainingArgs.removeFirst();
                if (isOneOf(arg, "--debug")) {
                    settings.trace = true;
                } else if (isOneOf(arg, "-v", "--verbose")) {
                    settings.verbose = true;
                } else if (isOneOf(arg, "-p", "--path")) {
                    processPathArg();
                } else if (isOneOf(arg, "-c", "--config")) {
                    processConfigFileArg();
                } else if (arg.startsWith("-")) {
                    throw new BadUsageException("Invalid flag: " + arg);
                } else {
                    settings.targets.add(arg);
                }
            }
        }

        private boolean haveHelpArg() {
            for (String arg : remainingArgs) {
                if (isOneOf(arg, "-h", "--help")) {
                    return true;
                }
            }
            return false;
        }

        private void requireCommand() {
            command = requireArg("Command argument required");
            String[] validCommands = {"verify"};
            if (!Arrays.asList(validCommands).contains(command)) {
                throw new BadUsageException("Invalid command '" + command + "'");
            }
        }
        
        private String requireArg(String msg) {
            if (remainingArgs.isEmpty()) {
                throw new BadUsageException(msg);
            }
            return remainingArgs.removeFirst();
        }
    
        private void processPathArg() {
            String[] parts = requireArg("Missing classpath").split(":");
            settings.searchPath = Arrays.asList(parts);
            checkSearchPath();
        }
        
        private void checkSearchPath() {
            for (String s : settings.searchPath) {
                if (!s.endsWith(".jar") && !s.endsWith("/") && !s.endsWith(File.separator)) {
                    throw new BadUsageException("Search path element '" + s + "' should have ended with '.jar' or a slash");
                }
            }
        }
        
        private void processConfigFileArg() {
            String file = requireArg("Config file name expected");
            configFileLoader.loadConfig(new File(file));
        }

        private boolean isOneOf(String arg, String... variants) {
            for (String s : variants) {
                if (arg.equals(s)) {
                    return true;
                }
            }
            return false;
        }
        
        private void checkUsage() {
            if (!settings.help) {
                if (settings.targets.isEmpty()) {
                    throw new BadUsageException("No targets given");
                }
            }
        }
    }
}
