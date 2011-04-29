package org.portablejava.app;

import java.util.ArrayList;
import java.util.List;

import org.portablejava.analysis.StrictfpSafetyAnalyzer;
import org.portablejava.analysis.results.BasicCallGraphAnalysis;
import org.portablejava.analysis.results.StrictfpSafetyAnalysis;
import org.portablejava.callgraph.CallGraphBuilder;
import org.portablejava.callgraph.Root;
import org.portablejava.ignoreset.EmptyIgnoreSet;
import org.portablejava.loaders.ClassFileLoader;
import org.portablejava.loaders.ClassPathClassFileLoader;
import org.portablejava.loaders.DefaultClassFileLoader;

public class Main {
    public static void main(String[] args) throws Exception {
        Settings settings = readArgs(args);
        new Main(settings).run();
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
    
    
    private Settings settings;
    private Appendable output;
    
    private List<Root> roots;
    private ClassFileLoader classFileLoader;
    
    private Main(Settings settings) {
        this.settings = settings;
        this.output = System.out;
    }
    
    private void run() throws Exception {
        roots = parseRoots();
        classFileLoader = makeClassFileLoader();
        BasicCallGraphAnalysis basicResult = buildCallgraph(classFileLoader);
        StrictfpSafetyAnalysis sfpResult = doStrictfpSafetyAnalysis(classFileLoader, basicResult);
        new Reporter(settings).writeReport(output, roots, sfpResult);
    }
    
    private List<Root> parseRoots() {
        List<Root> roots = new ArrayList<Root>();
        for (String target : settings.targets) {
            Root root;
            if (target.contains("::")) {
                String[] parts = target.split("::");
                root = new Root(parts[0], parts[1]);
            } else {
                root = new Root(target.replace('.', '/'));
            }
            roots.add(root);
        }
        return roots;
    }

    private ClassFileLoader makeClassFileLoader() {
        if (settings.searchPath != null) {
            return new ClassPathClassFileLoader(settings.searchPath);
        } else {
            return new DefaultClassFileLoader();
        }
    }
    
    private BasicCallGraphAnalysis buildCallgraph(ClassFileLoader classFileLoader) throws Exception {
        CallGraphBuilder builder = new CallGraphBuilder(classFileLoader, new EmptyIgnoreSet());
        builder.setDebugTrace(settings.trace);
        if (settings.verbose) {
            System.out.println("Building call graph...");
        }
        for (Root root : roots) {
            builder.addRoot(root);
        }
        return builder.getResult();
    }
    
    private StrictfpSafetyAnalysis doStrictfpSafetyAnalysis(ClassFileLoader classFileLoader, BasicCallGraphAnalysis basicResult) throws Exception {
        StrictfpSafetyAnalyzer analyzer = new StrictfpSafetyAnalyzer(basicResult);
        if (settings.verbose) {
            System.out.println("Analyzing strictfp safety...");
        }
        for (Root root : roots) {
            analyzer.addRoot(root);
        }
        return analyzer.getResult();
    }
}
