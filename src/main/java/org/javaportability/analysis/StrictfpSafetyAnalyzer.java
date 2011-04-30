package org.javaportability.analysis;

import org.javaportability.analysis.results.BasicCallGraphAnalysis;
import org.javaportability.analysis.results.CallPath;
import org.javaportability.analysis.results.StrictfpSafetyAnalysis;
import org.javaportability.callgraph.Root;
import org.javaportability.callgraph.CallGraph.CallSite;
import org.javaportability.callgraph.CallGraph.ClassNode;
import org.javaportability.callgraph.CallGraph.MethodNode;

public class StrictfpSafetyAnalyzer {
    private StrictfpSafetyAnalysis result;
    
    public StrictfpSafetyAnalyzer(BasicCallGraphAnalysis basic) {
        this.result = new StrictfpSafetyAnalysis(basic);
    }
    
    public StrictfpSafetyAnalysis getResult() {
        return result;
    }
    
    public void addRoot(Root root) {
        ClassNode cls = result.callGraph.getClass(root.getClassName());
        for (MethodNode m : cls.getMethodsIncludingInherited()) {
            if (root.matchesMethod(m)) {
                analyzeMethod(m);
            }
        }
    }
    
    private void analyzeMethod(MethodNode method) {
        if (!isAnalyzed(method)) {
            markAnalyzed(method);
            if (!isAssumedSafe(method)) {
                if (!isLocallySafe(method)) {
                    result.unsafeCallPaths.put(method, new CallPath(method));
                } else {
                    for (CallSite call : method.getOutgoingCalls()) {
                        MethodNode to = call.getTo();
                        analyzeMethod(to);
                        CallPath unsafe = result.unsafeCallPaths.get(to);
                        if (unsafe != null) {
                            result.unsafeCallPaths.put(method, new CallPath(method, unsafe));
                        }
                    }
                }
            }
        }
    }
    
    private boolean isAnalyzed(MethodNode method) {
        return result.strictfpAnalysisDoneMethods.contains(method);
    }

    private void markAnalyzed(MethodNode method) {
        result.strictfpAnalysisDoneMethods.add(method);
    }

    private boolean isLocallySafe(MethodNode node) {
        return !isAssumedUnsafe(node) && (!doesFpMathLocally(node) || isStrictfp(node) || isWhitelisted(node));
    }
    
    private boolean isAssumedSafe(MethodNode node) {
        return result.settings.assumedSafe.containsMethod(node.getPath());
    }
    
    private boolean isAssumedUnsafe(MethodNode node) {
        return result.settings.assumedUnsafe.containsMethod(node.getPath());
    }

    private boolean doesFpMathLocally(MethodNode node) {
        return result.localFpMathMethods.contains(node);
    }

    private boolean isStrictfp(MethodNode node) {
        return result.strictfpMethods.contains(node);
    }

    private boolean isWhitelisted(MethodNode node) {
        return result.settings.fpmathWhitelist.containsMethod(node.getPath());
    }
}
