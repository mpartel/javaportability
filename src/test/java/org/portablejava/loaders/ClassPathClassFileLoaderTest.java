package org.portablejava.loaders;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.portablejava.TestUtils;

public class ClassPathClassFileLoaderTest {
    
    private static final String TEST_DATA_DIR = TestUtils.TEST_DATA_DIR;
    private ClassPathClassFileLoader loader;
    
    @Before
    public void setUp() {
        String[] searchPath = {
            TEST_DATA_DIR + "/empty_dir/",
            TEST_DATA_DIR + "/nonexistent_dir/",
            TEST_DATA_DIR + "/",
            TEST_DATA_DIR + "/nonexistentJar.jar",
            TEST_DATA_DIR + "/withNoClasses.jar",
            TEST_DATA_DIR + "/withOneClass.jar"
        };
        loader = new ClassPathClassFileLoader(searchPath);
    }
    
    @Test
    public void testLoadingFromFile() throws ClassNotFoundException, IOException {
        testLoadingClass("pkg_not_in_jar/ClassNotInJar");
    }

    @Test
    public void testLoadingFromJars() throws ClassNotFoundException, IOException {
        testLoadingClass("pkg_in_jar/ClassInJar");
    }
    
    @Test
    public void testLoadingMultipleTimes() throws ClassNotFoundException, IOException {
        testLoadingClass("pkg_in_jar/ClassInJar");
        testLoadingClass("pkg_not_in_jar/ClassNotInJar");
        testLoadingClass("pkg_in_jar/ClassInJar");
        testLoadingClass("pkg_not_in_jar/ClassNotInJar");
    }
    
    @Test(expected = ClassNotFoundException.class)
    public void testNotFindingClass() throws ClassNotFoundException, IOException {
        loader.loadClass("empty_pkg/ThisDoesNotExist");
    }
    
    private void testLoadingClass(String name) throws ClassNotFoundException, IOException {
        ClassReader reader = loader.loadClass(name);
        assertEquals(name, reader.getClassName());
    }
}
