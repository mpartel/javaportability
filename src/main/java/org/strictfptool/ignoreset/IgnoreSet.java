package org.strictfptool.ignoreset;

import org.strictfptool.misc.MethodPath;

/**
 * A set of classes and/or methods to ignore.
 * If a class is ignored then all its methods are also ignored.
 * Implementations should be consistent with this.
 */
public interface IgnoreSet {
    public boolean containsClass(String className);
    public boolean containsMethod(MethodPath path);
}
