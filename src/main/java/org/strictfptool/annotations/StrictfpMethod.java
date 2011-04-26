package org.strictfptool.annotations;

import org.strictfptool.callgraph.CallGraph.MethodAnnotation;

public class StrictfpMethod implements MethodAnnotation {
    private static final StrictfpMethod instance = new StrictfpMethod();
    
    private StrictfpMethod() {
    }
    
    public static StrictfpMethod getInstance() {
        return instance;
    }
}
