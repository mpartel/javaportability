package org.strictfptool.loaders;

import java.io.IOException;

import org.objectweb.asm.ClassReader;

public interface ClassFileLoader {
    public ClassReader loadClass(String internalName) throws IOException;
}
