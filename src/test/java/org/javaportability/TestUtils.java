package org.javaportability;

import java.io.File;

public class TestUtils {
    public static final String ROOT_DIR;
    public static final String TEST_DATA_DIR;
    
    static {
        String thisClass = TestUtils.class.getName().replace('.', '/') + ".class";
        File thisClassFile = new File(ClassLoader.getSystemResource(thisClass).getPath());
        File dir = thisClassFile.getAbsoluteFile();
        int thisClassDepth = 6;
        for (int i = 0; i < thisClassDepth; ++i) {
            dir = dir.getParentFile();
        }
        ROOT_DIR = dir.getAbsolutePath();
        TEST_DATA_DIR = ROOT_DIR + "/src/test/data";
    }
}
