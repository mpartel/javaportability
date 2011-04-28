package org.lockstepcheck.analysis.results;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lockstepcheck.callgraph.CallGraph;
import org.lockstepcheck.callgraph.CallGraph.MethodNode;

public class StrictfpSafetyAnalysis extends BasicCallGraphAnalysis {

    protected Set<MethodNode> strictfpAnalysisDoneMethods;
    protected Map<MethodNode, CallPath> unsafeCalls;
    
    public StrictfpSafetyAnalysis(CallGraph callGraph) {
        super(callGraph);
        init();
    }
    
    public StrictfpSafetyAnalysis(BasicCallGraphAnalysis source) {
        super(source);
        init();
    }
    
    private void init() {
        this.strictfpAnalysisDoneMethods = new HashSet<MethodNode>();
        this.unsafeCalls = new HashMap<MethodNode, CallPath>();
    }
    
    public Set<MethodNode> strictfpAnalysisDoneMethods() {
        return strictfpAnalysisDoneMethods;
    }
    
    public Map<MethodNode, CallPath> unsafeCallPaths() {
        return unsafeCalls;
    }
    
}
