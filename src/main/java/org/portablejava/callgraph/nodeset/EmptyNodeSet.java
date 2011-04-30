package org.portablejava.callgraph.nodeset;

import org.portablejava.misc.MethodPath;

public final class EmptyNodeSet implements NodeSet {
    
    public EmptyNodeSet() {
    }
    
    @Override
    public boolean containsClass(String className) {
        return false;
    }
    
    @Override
    public boolean containsMethod(MethodPath path) {
        return false;
    }
}
