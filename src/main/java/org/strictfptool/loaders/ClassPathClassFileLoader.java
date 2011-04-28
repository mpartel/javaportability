package org.strictfptool.loaders;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.objectweb.asm.ClassReader;

public class ClassPathClassFileLoader implements ClassFileLoader {
    
    private HashMap<String, JarFile> jarCache;
    private String[] searchPath;
    
    public ClassPathClassFileLoader(String[] searchPath) {
        this.searchPath = searchPath;
    }
    
    public ClassPathClassFileLoader(List<String> searchPath) {
        this(searchPath.toArray(new String[searchPath.size()]));
    }
    
    @Override
    public ClassReader loadClass(String internalName) throws IOException {
        InputStream inputStream = null;
        for (String location : searchPath) {
            if (location.endsWith(".jar")) {
                inputStream = tryLoadFromJar(location, internalName);
            } else {
                inputStream = tryLoadFromFile(location, internalName);
            }
            if (inputStream != null) {
                break;
            }
        }
        if (inputStream != null) {
            return new ClassReader(inputStream);
        } else {
            throw new IOException("Could not find class: " + internalName); //TODO: another exception type
        }
    }

    private InputStream tryLoadFromJar(String location, String internalName) throws IOException {
        JarFile jf = getJarFile(location);
        ZipEntry entry = jf.getEntry(internalName + ".class");
        if (entry != null) {
            return new BufferedInputStream(jf.getInputStream(entry));
        } else {
            return null;
        }
    }

    private JarFile getJarFile(String location) throws IOException {
        if (!jarCache.containsKey(location)) {
            jarCache.put(location, new JarFile(location));
        }
        return jarCache.get(location);
    }

    private InputStream tryLoadFromFile(String location, String internalName) throws IOException {
        File file = new File(location + File.separator + internalName + ".class");
        if (file.exists()) {
            return new BufferedInputStream(new FileInputStream(file));
        } else {
            return null;
        }
    }
}
