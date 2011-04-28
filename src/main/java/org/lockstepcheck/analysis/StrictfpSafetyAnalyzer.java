package org.lockstepcheck.analysis;

import org.lockstepcheck.analysis.results.BasicCallGraphAnalysis;
import org.lockstepcheck.analysis.results.CallPath;
import org.lockstepcheck.analysis.results.StrictfpSafetyAnalysis;
import org.lockstepcheck.callgraph.CallGraph.CallSite;
import org.lockstepcheck.callgraph.CallGraph.MethodNode;

public class StrictfpSafetyAnalyzer {
    private StrictfpSafetyAnalysis result;
    
    public StrictfpSafetyAnalyzer(BasicCallGraphAnalysis basic) {
        this.result = new StrictfpSafetyAnalysis(basic);
    }
    
    public StrictfpSafetyAnalysis getResult() {
        return result;
    }
    
    public void analyzeMethod(MethodNode method) {
        if (!isAnalyzed(method)) {
            markAnalyzed(method);
            if (!isLocallySafe(method)) {
                result.unsafeCallPaths().put(method, new CallPath(method));
            } else {
                for (CallSite call : method.getOutgoingCalls()) {
                    MethodNode to = call.getTo();
                    analyzeMethod(to);
                    CallPath unsafe = result.unsafeCallPaths().get(to);
                    if (unsafe != null) {
                        result.unsafeCallPaths().put(method, new CallPath(method, unsafe));
                    }
                }
            }
        }
    }
    
    private boolean isAnalyzed(MethodNode method) {
        return result.strictfpAnalysisDoneMethods().contains(method);
    }

    private void markAnalyzed(MethodNode method) {
        result.strictfpAnalysisDoneMethods().add(method);
    }

    private boolean isLocallySafe(MethodNode node) {
        boolean localFp = result.localFpMathMethods().contains(node);
        boolean sfp = result.strictfpMethods().contains(node);
        return !localFp || sfp;
    }
}