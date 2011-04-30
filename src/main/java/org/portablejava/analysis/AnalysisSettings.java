package org.portablejava.analysis;

import org.portablejava.callgraph.nodeset.EmptyNodeSet;
import org.portablejava.callgraph.nodeset.MinimalIgnoreSet;
import org.portablejava.callgraph.nodeset.NodeSet;
import org.portablejava.callgraph.nodeset.NodeSets;
import org.portablejava.loaders.ClassFileLoader;

public class AnalysisSettings {
    public final ClassFileLoader classFileLoader;
    public final NodeSet ignoreSet;
    public final NodeSet blacklist;
    public final NodeSet fpmathWhitelist;
    
    public AnalysisSettings(ClassFileLoader classFileLoader) {
        this(classFileLoader, new EmptyNodeSet(), new EmptyNodeSet(), new EmptyNodeSet());
    }
    
    public AnalysisSettings(ClassFileLoader classFileLoader, NodeSet ignoreSet, NodeSet fpmathWhitelist, NodeSet blacklist) {
        this.classFileLoader = classFileLoader;
        this.ignoreSet = NodeSets.union(new MinimalIgnoreSet(), ignoreSet);
        this.fpmathWhitelist = fpmathWhitelist;
        this.blacklist = blacklist;
    }
}
