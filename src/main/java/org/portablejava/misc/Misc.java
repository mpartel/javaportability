package org.portablejava.misc;

import org.portablejava.callgraph.CallGraph.ClassNode;

public class Misc {
    public static String shortClassName(ClassNode node) {
        return shortClassName(node.getName());
    }

    private static String shortClassName(String className) {
        String[] parts = className.split("[/.]");
        return parts[parts.length - 1];
    }
}
