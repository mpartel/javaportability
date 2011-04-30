package org.javaportability.callgraph.nodeset;

import org.javaportability.misc.MethodPath;

/**
 * A set of class and/or method names. Used as whitelists, blacklists, ignorelists etc.
 * 
 * If a class is in a set then all its methods should also be in the set.
 */
public interface NodeSet {
    public boolean containsClass(String className);
    public boolean containsMethod(MethodPath path);
}
