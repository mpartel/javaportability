package org.portablejava.analysis;

import org.portablejava.analysis.results.BasicCallGraphAnalysis;
import org.portablejava.analysis.results.CallPath;
import org.portablejava.analysis.results.StrictfpSafetyAnalysis;
import org.portablejava.callgraph.CallGraph.CallSite;
import org.portablejava.callgraph.CallGraph.ClassNode;
import org.portablejava.callgraph.CallGraph.MethodNode;
import org.portablejava.callgraph.Root;

public class StrictfpSafetyAnalyzer {
    private StrictfpSafetyAnalysis result;
    
    public StrictfpSafetyAnalyzer(BasicCallGraphAnalysis basic) {
        this.result = new StrictfpSafetyAnalysis(basic);
    }
    
    public StrictfpSafetyAnalysis getResult() {
        return result;
    }
    
    public void addRoot(Root root) {
        ClassNode cls = result.callGraph().getClass(root.getClassName());
        for (MethodNode m : cls.getMethodsIncludingInherited()) {
            if (root.matchesMethod(m)) {
                analyzeMethod(m);
            }
        }
    }
    
    private void analyzeMethod(MethodNode method) {
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
        return !isInherentlyUnsafe(node) && (!doesFpMathLocally(node) || isStrictfp(node) || isWhitelisted(node));
    }
    
    private boolean isInherentlyUnsafe(MethodNode node) {
        return result.settings().inherentlyUnsafe.containsMethod(node.getPath());
    }

    private boolean doesFpMathLocally(MethodNode node) {
        return result.localFpMathMethods().contains(node);
    }

    private boolean isStrictfp(MethodNode node) {
        return result.strictfpMethods().contains(node);
    }

    private boolean isWhitelisted(MethodNode node) {
        return result.settings().fpmathWhitelist.containsMethod(node.getPath());
    }
}
