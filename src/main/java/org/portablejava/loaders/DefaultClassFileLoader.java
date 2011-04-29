package org.portablejava.loaders;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;

public class DefaultClassFileLoader implements ClassFileLoader {
    @Override
    public ClassReader loadClass(String internalName) throws ClassNotFoundException, IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream(internalName + ".class");
        if (is != null) {
            return new ClassReader(is);
        } else {
            throw new ClassNotFoundException(internalName);
        }
    }
}
