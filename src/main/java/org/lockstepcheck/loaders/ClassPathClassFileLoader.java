package org.lockstepcheck.loaders;

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
    
    private String[] searchPath;
    private HashMap<String, JarFile> jarCache;
    
    public ClassPathClassFileLoader(String[] searchPath) {
        this.searchPath = searchPath;
        this.jarCache = new HashMap<String, JarFile>();
    }
    
    public ClassPathClassFileLoader(List<String> searchPath) {
        this(searchPath.toArray(new String[searchPath.size()]));
    }
    
    @Override
    public ClassReader loadClass(String internalName) throws ClassNotFoundException, IOException {
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
            throw new ClassNotFoundException("Could not find class: " + internalName);
        }
    }

    private InputStream tryLoadFromJar(String location, String internalName) throws IOException {
        JarFile jf = getJarFile(location);
        ZipEntry entry = jf != null ? jf.getEntry(internalName + ".class") : null;
        if (entry != null) {
            return new BufferedInputStream(jf.getInputStream(entry));
        } else {
            return null;
        }
    }

    private JarFile getJarFile(String location) throws IOException {
        if (jarCache.containsKey(location)) {
            return jarCache.get(location);
        } else {
            File file = new File(location);
            JarFile jf = file.exists() ? new JarFile(location) : null;
            jarCache.put(location, jf);
            return jf;
        }
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
