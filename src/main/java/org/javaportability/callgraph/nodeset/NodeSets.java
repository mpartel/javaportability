package org.javaportability.callgraph.nodeset;

import org.javaportability.misc.MethodPath;

public class NodeSets {
    public static NodeSet complement(final NodeSet s) {
        return new NodeSet() {
            @Override
            public boolean containsMethod(MethodPath path) {
                return !s.containsMethod(path);
            }
            
            @Override
            public boolean containsClass(String className) {
                return !s.containsClass(className);
            }
        };
    }
    
    public static NodeSet union(final NodeSet a, final NodeSet b) {
        return new NodeSet() {
            @Override
            public boolean containsClass(String className) {
                return a.containsClass(className) || b.containsClass(className);
            }

            @Override
            public boolean containsMethod(MethodPath path) {
                return a.containsMethod(path) || b.containsMethod(path);
            }
        };
    }
}
