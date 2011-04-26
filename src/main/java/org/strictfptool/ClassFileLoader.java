package org.strictfptool;

import java.io.IOException;

import org.objectweb.asm.ClassReader;

public interface ClassFileLoader {
    public ClassReader loadClass(String internalName) throws IOException;
}
