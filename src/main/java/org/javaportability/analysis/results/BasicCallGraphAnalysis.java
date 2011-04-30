package org.javaportability.analysis.results;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.javaportability.analysis.AnalysisSettings;
import org.javaportability.callgraph.CallGraph;
import org.javaportability.callgraph.CallGraphBuilder;
import org.javaportability.callgraph.CallGraph.MethodNode;

/**
 * A record of the immediate information obtained by {@link CallGraphBuilder}.
 */
public class BasicCallGraphAnalysis {
    public AnalysisSettings settings;
    public CallGraph callGraph;
    public Set<MethodNode> localFpMathMethods;
    public Set<MethodNode> nativeMethods;
    public Set<MethodNode> strictfpMethods;
    public Set<MethodNode> basicAnalysisDoneMethods;
    
    public BasicCallGraphAnalysis(AnalysisSettings settings, CallGraph callGraph) {
        this.settings = settings;
        this.callGraph = callGraph;
        this.localFpMathMethods = new HashSet<MethodNode>();
        this.nativeMethods = new HashSet<MethodNode>();
        this.strictfpMethods = new HashSet<MethodNode>();
        this.basicAnalysisDoneMethods = new HashSet<MethodNode>();
    }
    
    protected BasicCallGraphAnalysis(BasicCallGraphAnalysis source) {
        this.settings = source.settings;
        this.callGraph = source.callGraph;
        this.localFpMathMethods = Collections.unmodifiableSet(source.localFpMathMethods);
        this.nativeMethods = Collections.unmodifiableSet(source.nativeMethods);
        this.strictfpMethods = Collections.unmodifiableSet(source.strictfpMethods);
        this.basicAnalysisDoneMethods = Collections.unmodifiableSet(source.basicAnalysisDoneMethods);
    }
}
