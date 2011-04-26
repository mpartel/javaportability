package org.strictfptool;

import java.io.IOException;

import org.objectweb.asm.ClassReader;

public class DefaultClassFileLoader implements ClassFileLoader {
    @Override
    public ClassReader loadClass(String internalName) throws IOException {
        return new ClassReader(internalName);
    }
}
