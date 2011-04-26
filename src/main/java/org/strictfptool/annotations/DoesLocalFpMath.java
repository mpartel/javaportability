package org.strictfptool.annotations;

import org.strictfptool.callgraph.CallGraph.MethodAnnotation;

public class DoesLocalFpMath implements MethodAnnotation {
    private static final DoesLocalFpMath instance = new DoesLocalFpMath();
    
    private DoesLocalFpMath() {
    }
    
    public static DoesLocalFpMath getInstance() {
        return instance;
    }
}
