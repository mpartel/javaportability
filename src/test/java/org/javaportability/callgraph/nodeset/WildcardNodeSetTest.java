package org.javaportability.callgraph.nodeset;

import static org.junit.Assert.*;

import org.javaportability.misc.MethodPath;
import org.junit.Test;

public class WildcardNodeSetTest {
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
        
        assertMatches("*.lang.*", new MethodPath("java/lang/Object", "toString", "()V"));
        assertNoMatch("*.lang.*", new MethodPath("java/util/List", "toString", "()V"));
        
        assertMatches("*lang*", "java/lang/Object");
        assertNoMatch("*lang*", "java/util/List");
    }
    
    @Test
    public void testNoWildcards() {
        assertMatches("java.lang.Object", "java/lang/Object");
        assertNoMatch("java.lang.String", "java/lang/Object");
        
        assertMatches("java.lang.Object", new MethodPath("java/lang/Object", "toString", "()V"));
        assertNoMatch("java.lang.String", new MethodPath("java/lang/Object", "toString", "()V"));
    }
    
    @Test
    public void testMethods() {
        MethodPath toString = new MethodPath("java/lang/String", "toString", "()V");
        MethodPath indexOf = new MethodPath("java/lang/String", "indexOf", "(I)I");
        assertMatches("java.lang.String::toString", toString);
        assertNoMatch("java.lang.String::toString", indexOf);
        
        assertMatches("java.lang.*::toString", toString);
        assertNoMatch("java.lang.*::toString", indexOf);
        
        assertMatches("java.lang.String::to*", toString);
        assertNoMatch("java.lang.String::to*", indexOf);
        
        assertMatches("java.lang.*::to*", toString);
        assertNoMatch("java.lang.*::to*", indexOf);
        
        assertMatches("java.lang.*::*", indexOf);
        assertMatches("*::*", indexOf);
        assertMatches("*", indexOf);
    }

    private void assertMatches(String pattern, MethodPath mp) {
        if (!new WildcardNodeSet(pattern).containsMethod(mp)) {
            fail("Expected wildcard " + pattern + " to match " + mp);
        }
    }
    
    private void assertNoMatch(String pattern, MethodPath mp) {
        if (new WildcardNodeSet(pattern).containsMethod(mp)) {
            fail("Didn't expect wildcard " + pattern + " to match " + mp);
        }
    }
    
    private void assertMatches(String pattern, String cls) {
        if (!new WildcardNodeSet(pattern).containsClass(cls)) {
            fail("Expected wildcard " + pattern + " to match " + cls);
        }
    }
    
    private void assertNoMatch(String pattern, String cls) {
        if (new WildcardNodeSet(pattern).containsClass(cls)) {
            fail("Didn't expect wildcard " + pattern + " to match " + cls);
        }
    }
}
