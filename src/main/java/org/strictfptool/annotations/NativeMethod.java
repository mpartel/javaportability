package org.strictfptool.annotations;

import org.strictfptool.callgraph.CallGraph.MethodAnnotation;

public class NativeMethod implements MethodAnnotation {
    private static final NativeMethod instance = new NativeMethod();
    
    private NativeMethod() {
    }
    
    public static NativeMethod getInstance() {
        return instance;
    }
}
