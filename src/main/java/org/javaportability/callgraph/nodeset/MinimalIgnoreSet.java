package org.javaportability.callgraph.nodeset;

import org.javaportability.misc.MethodPath;

/**
 * Contains array classes, since they can never be discovered.
 * This is used automatically when needed.
 */
public class MinimalIgnoreSet implements NodeSet {

    @Override
    public boolean containsClass(String className) {
        return className.startsWith("[");
    }

    @Override
    public boolean containsMethod(MethodPath path) {
        return containsClass(path.getOwner());
    }
    
}
