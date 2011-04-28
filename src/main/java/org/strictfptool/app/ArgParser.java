package org.strictfptool.app;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

public class ArgParser {

    public Settings parseArgs(String[] args) {
        Impl impl = new Impl(args);
        impl.processArgs();
        impl.checkUsage();
        return impl.settings;
    }
    
    public String getUsage() {
        return
            "Usage: java -jar strictfp-tool.jar [options] target [target...]\n" +
            "\n" +
            "Targets can be class names or methods denoted \"pkg.Class::method\"\n" +
            "\n" +
            "Options:\n" +
            "  -p, --path class:path:components    Class path to search classes from.\n" +
            "                                      Omit to use the JVM's classpath.\n" +
            "  -h, --help                          This help message.";
    }
    
    private static class Impl {
        private Settings settings;
        private LinkedList<String> remainingArgs;
        
        private Impl(String[] args) {
            settings = new Settings();
            remainingArgs = new LinkedList<String>(Arrays.asList(args));
        }
        
        private void processArgs() {
            while (!remainingArgs.isEmpty()) {
                String arg = remainingArgs.removeFirst();
                if (isOneOf(arg, "-h", "--help")) {
                    settings.help = true;
                } else if (isOneOf(arg, "-p", "--path")) {
                    processPathArg();
                } else {
                    settings.targets.add(arg);
                }
            }
        }
    
        private void checkUsage() {
            if (!settings.help) {
                if (settings.targets.isEmpty()) {
                    throw new BadUsageException("No targets given");
                }
            }
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

        private String requireArg(String msg) {
            if (remainingArgs.isEmpty()) {
                throw new BadUsageException(msg);
            }
            return remainingArgs.removeFirst();
        }
    
        private boolean isOneOf(String arg, String... variants) {
            for (String s : variants) {
                if (arg.equals(s)) {
                    return true;
                }
            }
            return false;
        }
    }
}
