package org.javaportability.analysis;

import org.javaportability.callgraph.nodeset.EmptyNodeSet;
import org.javaportability.callgraph.nodeset.NodeSet;
import org.javaportability.loaders.ClassFileLoader;

public class AnalysisSettings {
    public ClassFileLoader classFileLoader;
    public NodeSet ignoreSet;
    public NodeSet assumedSafe;
    public NodeSet assumedUnsafe;
    public NodeSet allowedFpMath;
    
    public AnalysisSettings(ClassFileLoader classFileLoader) {
        this.classFileLoader = classFileLoader;
        this.ignoreSet = new EmptyNodeSet();
        this.assumedSafe = new EmptyNodeSet();
        this.assumedUnsafe = new EmptyNodeSet();
        this.allowedFpMath = new EmptyNodeSet();
    }
}
