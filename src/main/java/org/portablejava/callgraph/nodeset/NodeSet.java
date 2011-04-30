package org.portablejava.callgraph.nodeset;

import org.portablejava.misc.MethodPath;

/**
 * A set of classes and/or methods.
 * If a class is in a set then all its methods should also be in the set.
 */
public interface NodeSet {
    public boolean containsClass(String className);
    public boolean containsMethod(MethodPath path);
}
