package org.strictfptool.app;

import org.strictfptool.analysis.results.BasicCallGraphAnalysis;
import org.strictfptool.callgraph.CallGraphBuilder;
import org.strictfptool.ignoreset.EmptyIgnoreSet;
import org.strictfptool.loaders.ClassFileLoader;
import org.strictfptool.loaders.ClassPathClassFileLoader;
import org.strictfptool.loaders.DefaultClassFileLoader;

public class Main {
    public static void main(String[] args) throws Exception {
        Settings settings = readArgs(args);
        ClassFileLoader classFileLoader = makeClassFileLoader(settings);
        BasicCallGraphAnalysis basicResult = buildCallgraph(settings, classFileLoader);
        System.out.println("TODO...");
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
    
    private static BasicCallGraphAnalysis buildCallgraph(Settings settings, ClassFileLoader classFileLoader) throws Exception {
        CallGraphBuilder builder = new CallGraphBuilder(classFileLoader, EmptyIgnoreSet.getInstance());
        builder.setDebugTrace(settings.trace);
        
        for (String target : settings.targets) {
            if (target.contains("::")) {
                String[] parts = target.split("::");
                builder.addRootMethod(parts[0], parts[1]);
            } else {
                builder.addRootClass(target.replace('.', '/'));
            }
        }
        return builder.getResult();
    }
}
