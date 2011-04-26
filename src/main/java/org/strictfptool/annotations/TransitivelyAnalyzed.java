package org.strictfptool.annotations;

import org.strictfptool.callgraph.CallGraphBuilder;
import org.strictfptool.callgraph.CallGraph.MethodAnnotation;

/**
 * {@link CallGraphBuilder} has analyzed (transitively) all methods
 * called by this method.
 * 
 * Methods lacking this annotation were not called from the requested roots.
 */
public class TransitivelyAnalyzed implements MethodAnnotation {
    private static final TransitivelyAnalyzed instance = new TransitivelyAnalyzed();
    
    private TransitivelyAnalyzed() {
    }
    
    public static TransitivelyAnalyzed getInstance() {
        return instance;
    }
}
