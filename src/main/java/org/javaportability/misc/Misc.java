package org.javaportability.misc;

import org.javaportability.callgraph.CallGraph.ClassNode;

public class Misc {
    public static String shortClassName(ClassNode node) {
        return shortClassName(node.getName());
    }

    private static String shortClassName(String className) {
        String[] parts = className.split("[/.]");
        return parts[parts.length - 1];
    }
}
