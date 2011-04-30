package org.portablejava.analysis.results;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.portablejava.analysis.AnalysisSettings;
import org.portablejava.callgraph.CallGraph;
import org.portablejava.callgraph.CallGraph.MethodNode;

public class StrictfpSafetyAnalysis extends BasicCallGraphAnalysis {
    public Set<MethodNode> strictfpAnalysisDoneMethods;
    public Map<MethodNode, CallPath> unsafeCallPaths;
    
    public StrictfpSafetyAnalysis(AnalysisSettings settings, CallGraph callGraph) {
        super(settings, callGraph);
        init();
    }
    
    public StrictfpSafetyAnalysis(BasicCallGraphAnalysis source) {
        super(source);
        init();
    }
    
    private void init() {
        this.strictfpAnalysisDoneMethods = new HashSet<MethodNode>();
        this.unsafeCallPaths = new HashMap<MethodNode, CallPath>();
    }
}
