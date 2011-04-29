package org.portablejava.ignoreset;

import static org.junit.Assert.*;

import org.junit.Test;
import org.portablejava.ignoreset.WildcardIgnoreSet;

public class WildcardIgnoreSetTest {
    @Test
    public void testEndingWithWildcard() {
        assertMatches("java.*", "java/lang/Object");
        assertMatches("java.lang.*", "java/lang/Object");
        assertMatches("java.lang*", "java/lang/Object");
        assertMatches("*", "java/lang/Object");
        
        assertNoMatch("java.lang.*", "java/util/List");
        assertNoMatch("java.lang*", "java/util/List");
    }
    
    @Test
    public void testMultipleWildcards() {
        assertMatches("*.lang.*", "java/lang/Object");
        assertNoMatch("*.lang.*", "java/util/List");
        
        assertMatches("*lang*", "java/lang/Object");
        assertNoMatch("*lang*", "java/util/List");
    }
    
    @Test
    public void testNoWildcards() {
        assertMatches("java.lang.Object", "java/lang/Object");
        assertNoMatch("java.lang.String", "java/lang/Object");
    }
    
    private void assertMatches(String pattern, String cls) {
        if (!new WildcardIgnoreSet(pattern).containsClass(cls)) {
            fail("Expected wildcard " + pattern + " to match " + cls);
        }
    }
    
    private void assertNoMatch(String pattern, String cls) {
        if (new WildcardIgnoreSet(pattern).containsClass(cls)) {
            fail("Didn't expect wildcard " + pattern + " to match " + cls);
        }
    }
}
