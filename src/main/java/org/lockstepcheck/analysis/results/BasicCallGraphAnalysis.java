package org.lockstepcheck.analysis.results;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.lockstepcheck.callgraph.CallGraph;
import org.lockstepcheck.callgraph.CallGraph.MethodNode;
import org.lockstepcheck.callgraph.CallGraphBuilder;

/**
 * A record of the immediate information obtained by {@link CallGraphBuilder}.
 */
public class BasicCallGraphAnalysis {
    protected CallGraph callGraph;
    protected Set<MethodNode> localFpMathMethods;
    protected Set<MethodNode> nativeMethods;
    protected Set<MethodNode> strictfpMethods;
    protected Set<MethodNode> basicAnalysisDoneMethods;
    
    public BasicCallGraphAnalysis(CallGraph callGraph) {
        this.callGraph = callGraph;
        this.localFpMathMethods = new HashSet<MethodNode>();
        this.nativeMethods = new HashSet<MethodNode>();
        this.strictfpMethods = new HashSet<MethodNode>();
        this.basicAnalysisDoneMethods = new HashSet<MethodNode>();
    }
    
    protected BasicCallGraphAnalysis(BasicCallGraphAnalysis source) {
        this.callGraph = source.callGraph;
        this.localFpMathMethods = Collections.unmodifiableSet(source.localFpMathMethods);
        this.nativeMethods = Collections.unmodifiableSet(source.nativeMethods);
        this.strictfpMethods = Collections.unmodifiableSet(source.strictfpMethods);
        this.basicAnalysisDoneMethods = Collections.unmodifiableSet(source.basicAnalysisDoneMethods);
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
