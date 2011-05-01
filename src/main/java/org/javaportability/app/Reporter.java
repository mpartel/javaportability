package org.javaportability.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.javaportability.analysis.results.CallPath;
import org.javaportability.analysis.results.StrictfpSafetyAnalysis;
import org.javaportability.callgraph.CallGraph;
import org.javaportability.callgraph.Root;
import org.javaportability.callgraph.CallGraph.ClassNode;
import org.javaportability.callgraph.CallGraph.MethodNode;
import org.javaportability.misc.Misc;

public class Reporter {
    protected Settings settings;
    
    protected Appendable output;
    protected List<Root> roots;
    protected StrictfpSafetyAnalysis result;
    private List<MethodNode> rootMethods;
    
    public Reporter(Settings settings) {
        this.settings = settings;
    }
    
    public void writeReport(Appendable output, List<Root> roots, StrictfpSafetyAnalysis result) throws IOException {
        this.output = output;
        this.roots = roots;
        this.result = result;
        findRootMethods(result.callGraph);
        writeReport();
    }
    
    private void writeReport() throws IOException {
        writeln("Stats:");
        writeln("* Classes visited: " + result.callGraph.getClasses().size());
        writeln();
        if (result.unsafeCallPaths.isEmpty()) {
            writeln("All call paths seem safe.");
        } else {
            writeln("Unsafe call paths:");
            writeUnsafeCallPaths();
        }
        writeln();
    }

    private void writeUnsafeCallPaths() throws IOException {
        for (MethodNode root : rootMethods) {
            CallPath path = result.unsafeCallPaths.get(root);
            if (path != null) {
                writeUnsafePath(root, path);
            }
        }
    }

    private void writeUnsafePath(MethodNode root, CallPath path) throws IOException {
        Formatter formatter = new Formatter(output);
        ArrayList<String> leftColumn = getCallPathShortLines(path);
        ArrayList<String> rightColumn = getCallPathLongLines(path);
        int leftColumnWidth = maxLength(leftColumn);
         
        writeln("* " + root.getOwner() + "::" + root.getName() + " " + root.getDesc() + ":");
        for (int i = 1; i < leftColumn.size(); ++i) {
            String left = leftColumn.get(i);
            String right = rightColumn.get(i);
            formatter.format("  -> %-" + leftColumnWidth + "s    [%s]\n", left, right);
        }
        writeln();
    }
    
    private int maxLength(ArrayList<String> strs) {
        int max = 0;
        for (String s : strs) {
            int l = s.length();
            if (l > max) {
                max = l;
            }
        }
        return max;
    }

    private ArrayList<String> getCallPathShortLines(CallPath path) {
        ArrayList<String> lines = new ArrayList<String>();
        for (MethodNode m : path) {
            lines.add(shortDesc(m));
        }
        return lines;
    }

    private ArrayList<String> getCallPathLongLines(CallPath path) {
        ArrayList<String> lines = new ArrayList<String>();
        for (MethodNode m : path) {
            lines.add(longDesc(m));
        }
        return lines;
    }
    
    private String shortDesc(MethodNode m) {
        String shortClass = Misc.shortClassName(m.getOwner());
        String shortMethod = m.getName();
        String shortDesc = shortClass + "::" + shortMethod;
        return shortDesc;
    }

    private String longDesc(MethodNode m) {
        String longClass = m.getOwner().getName();
        String longMethod = m.getName() + " " + m.getDesc();
        String longDesc = longClass + " :: " + longMethod;
        return longDesc;
    }
    
    private void findRootMethods(CallGraph callGraph) {
        rootMethods = new ArrayList<MethodNode>();
        for (Root root : roots) {
            ClassNode cls = callGraph.getClass(root.getClassName());
            for (MethodNode m : cls.getLocalMethods()) {
                if (root.matchesMethod(m)) {
                    rootMethods.add(m);
                }
            }
        }
    }
    
    private void writeln(Object obj) throws IOException {
        write(obj);
        writeln();
    }
    
    private void writeln() throws IOException {
        write("\n");
    }
    
    private void write(Object obj) throws IOException {
        output.append(obj.toString());
    }
}
