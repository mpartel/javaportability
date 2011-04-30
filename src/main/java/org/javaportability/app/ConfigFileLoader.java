package org.javaportability.app;

import java.util.Scanner;

import org.javaportability.analysis.AnalysisSettings;
import org.javaportability.callgraph.nodeset.NodeSet;
import org.javaportability.callgraph.nodeset.NodeSets;
import org.javaportability.callgraph.nodeset.WildcardNodeSet;

public class ConfigFileLoader {
    private Settings settings;
    private Scanner scanner;
    
    public ConfigFileLoader(Settings settings) {
        this.settings = settings;
        this.scanner = null;
    }
    
    public void loadConfig(Readable source) {
        this.scanner = new Scanner(source);
        while (scanner.hasNextLine()) {
            processLine(scanner.nextLine());
        }
        this.scanner = null;
    }

    private void processLine(String line) {
        line = line.trim();
        if (shouldSkipLine(line)) {
            return;
        }
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0];
        String rest = (parts.length > 1 ? parts[1] : "");
        
        AnalysisSettings as = settings.analysisSettings;
        
        if (cmd.equals("ignore")) {
            as.ignoreSet = addToNodeSet(as.ignoreSet, rest);
        } else if (cmd.equals("safe")) {
            as.assumedSafe = addToNodeSet(as.assumedSafe, rest);
        } else if (cmd.equals("unsafe")) {
            as.assumedUnsafe = addToNodeSet(as.assumedUnsafe, rest);
        } else if (cmd.equals("allowfp")) {
            as.allowedFpMath = addToNodeSet(as.allowedFpMath, rest);
        } else {
            throw new BadUsageException("Invalid line in configuration file: '" + line + "'");
        }
    }

    private boolean shouldSkipLine(String line) {
        return line.isEmpty() || line.startsWith("#") || line.startsWith("//");
    }

    private NodeSet addToNodeSet(NodeSet set, String spec) {
        return NodeSets.union(set, new WildcardNodeSet(spec));
    }
}
