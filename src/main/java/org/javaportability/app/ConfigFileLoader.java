package org.javaportability.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.javaportability.analysis.AnalysisSettings;
import org.javaportability.callgraph.nodeset.NodeSet;
import org.javaportability.callgraph.nodeset.NodeSets;
import org.javaportability.callgraph.nodeset.WildcardNodeSet;

public class ConfigFileLoader {
    private static final Pattern commentPattern = Pattern.compile("(#|//).*$");
    private Settings settings;
    private Scanner scanner;
    
    public ConfigFileLoader(Settings settings) {
        this.settings = settings;
        this.scanner = null;
    }
    
    public void loadConfig(File file) {
        try {
            loadConfig(new BufferedReader(new FileReader(file)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void loadConfig(Readable source) {
        this.scanner = new Scanner(source);
        while (scanner.hasNextLine()) {
            processLine(scanner.nextLine());
        }
        this.scanner = null;
    }

    private void processLine(String line) {
        line = cleanLine(line);
        if (line.isEmpty()) {
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

    private String cleanLine(String line) {
        return commentPattern.matcher(line).replaceAll("").trim();
    }

    private NodeSet addToNodeSet(NodeSet set, String spec) {
        return NodeSets.union(set, new WildcardNodeSet(spec));
    }
}
