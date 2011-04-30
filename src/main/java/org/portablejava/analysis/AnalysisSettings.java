package org.portablejava.analysis;

import org.portablejava.callgraph.nodeset.EmptyNodeSet;
import org.portablejava.callgraph.nodeset.NodeSet;
import org.portablejava.loaders.ClassFileLoader;

public class AnalysisSettings {
    public ClassFileLoader classFileLoader;
    public NodeSet ignoreSet;
    public NodeSet assumedSafe;
    public NodeSet assumedUnsafe;
    public NodeSet fpmathWhitelist;
    
    public AnalysisSettings(ClassFileLoader classFileLoader) {
        this.classFileLoader = classFileLoader;
        this.ignoreSet = new EmptyNodeSet();
        this.assumedSafe = new EmptyNodeSet();
        this.assumedUnsafe = new EmptyNodeSet();
        this.fpmathWhitelist = new EmptyNodeSet();
    }
}
