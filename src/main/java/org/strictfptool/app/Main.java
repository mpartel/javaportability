package org.strictfptool.app;

import java.util.List;

import org.strictfptool.ClassFileLoader;
import org.strictfptool.ClassPathClassFileLoader;
import org.strictfptool.DefaultClassFileLoader;

public class Main {
    public static void main(String[] args) {
        Settings settings = readArgs(args);
        ClassFileLoader classFileLoader = makeClassFileLoader(settings);
        processTargets(settings.targets, classFileLoader);
    }

    private static Settings readArgs(String[] args) {
        ArgParser argParser = new ArgParser();
        Settings settings = null;
        try {
            settings = argParser.parseArgs(args);
        } catch (BadUsageException e) {
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println(argParser.getUsage());
            System.exit(1);
        }
        if (settings.help) {
            System.out.println(argParser.getUsage());
            System.exit(0);
        }
        return settings;
    }
    
    private static ClassFileLoader makeClassFileLoader(Settings settings) {
        if (settings.searchPath != null) {
            return new ClassPathClassFileLoader(settings.searchPath);
        } else {
            return new DefaultClassFileLoader();
        }
    }
    
    private static void processTargets(List<String> targets, ClassFileLoader classFileLoader) {
        for (String target : targets) {
            //TODO
        }
    }
}
