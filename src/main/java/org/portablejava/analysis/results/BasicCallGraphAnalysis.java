package org.portablejava.analysis.results;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.portablejava.analysis.AnalysisSettings;
import org.portablejava.callgraph.CallGraph;
import org.portablejava.callgraph.CallGraph.MethodNode;
import org.portablejava.callgraph.CallGraphBuilder;

/**
 * A record of the immediate information obtained by {@link CallGraphBuilder}.
 */
public class BasicCallGraphAnalysis {
    protected AnalysisSettings settings;
    protected CallGraph callGraph;
    protected Set<MethodNode> localFpMathMethods;
    protected Set<MethodNode> nativeMethods;
    protected Set<MethodNode> strictfpMethods;
    protected Set<MethodNode> basicAnalysisDoneMethods;
    
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
    
    public AnalysisSettings settings() {
        return settings;
    }
    
    public CallGraph callGraph() {
        return callGraph;
    }
    
    public Set<MethodNode> localFpMathMethods() {
        return localFpMathMethods;
    }
    
    public Set<MethodNode> nativeMethods() {
        return nativeMethods;
    }
    
    public Set<MethodNode> strictfpMethods() {
        return strictfpMethods;
    }
    
    public Set<MethodNode> basicAnalysisDoneMethods() {
        return basicAnalysisDoneMethods;
    }
}
