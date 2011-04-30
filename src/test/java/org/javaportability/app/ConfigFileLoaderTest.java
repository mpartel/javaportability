package org.javaportability.app;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.javaportability.analysis.AnalysisSettings;
import org.javaportability.misc.MethodPath;
import org.junit.Before;
import org.junit.Test;

public class ConfigFileLoaderTest {
    
    private Settings settings;
    private AnalysisSettings as;
    private ConfigFileLoader loader;
    
    @Before
    public void setUp() {
        settings = new Settings();
        as = settings.analysisSettings = new AnalysisSettings(null);
        loader = new ConfigFileLoader(settings);
    }
    
    @Test
    public void testNodeSets() {
        load("ignore java.lang.*",
             "ignore *Hash*",
             "",
             "safe *Foo*",
             "unsafe *Bar*",
             "allowfp *Foo*");
        
        assertTrue(as.ignoreSet.containsClass("java/lang/String"));
        assertTrue(as.ignoreSet.containsMethod(mp("java/lang/String::codePointAt (I)I")));
        assertTrue(as.ignoreSet.containsClass("java/util/HashMap"));
        assertFalse(as.ignoreSet.containsClass("java/util/LinkedList"));
        
        assertTrue(as.assumedSafe.containsClass("Foo"));
        assertFalse(as.assumedUnsafe.containsClass("Foo"));
        
        assertFalse(as.assumedSafe.containsClass("Bar"));
        assertTrue(as.assumedUnsafe.containsClass("Bar"));
        
        assertTrue(as.assumedSafe.containsClass("FooBar"));
        assertTrue(as.assumedUnsafe.containsClass("FooBar"));
        
        assertTrue(as.allowedFpMath.containsClass("AFooBar"));
        assertFalse(as.allowedFpMath.containsClass("ABar"));
    }
    
    @Test
    public void testAllowFp() {
        load("allowfp *Foo*");
    }
    
    @Test
    public void testComments() {
        load("# this is a comment",
             "ignore *",
             "// as is this");
        assertTrue(as.ignoreSet.containsClass("anything"));
    }
    
    
    private MethodPath mp(String string) {
        String[] parts1 = string.split("::");
        String[] parts2 = parts1[1].split(" ");
        return new MethodPath(parts1[0], parts2[0], parts2[1]);
    }

    private void load(String... fileLines) {
        StringBuilder sb = new StringBuilder();
        for (String line : fileLines) {
            sb.append(line);
            sb.append("\n");
        }
        loader.loadConfig(new StringReader(sb.toString()));
    }
}
