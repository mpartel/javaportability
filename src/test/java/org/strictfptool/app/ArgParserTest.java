package org.strictfptool.app;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class ArgParserTest {
    
    private Settings parseArgs(String... args) {
        return new ArgParser().parseArgs(args);
    }
    
    @Test
    public void testHelp() {
        assertTrue(parseArgs("-h").help);
        assertTrue(parseArgs("--help").help);
    }
    
    @Test
    public void testSearchPathWithSpace() {
        List<String> sp = parseArgs("-p", "foo/foo/:bar.jar", "Target").searchPath;
        assertEquals(2, sp.size());
        assertEquals("foo/foo/", sp.get(0));
        assertEquals("bar.jar", sp.get(1));
    }
    
    @Test(expected = BadUsageException.class)
    public void testSearchPathEntryMissingTrailingSlashAndJarExtension() {
        parseArgs("-p", "foo/foo:bar.jar", "Target");
    }
    
    @Test
    public void testCombined() {
        Settings settings = parseArgs("--path", "xoo/foo.jar", "One", "Two::method");
        assertEquals(1, settings.searchPath.size());
        assertEquals("xoo/foo.jar", settings.searchPath.get(0));
        assertEquals("One", settings.targets.get(0));
        assertEquals("Two::method", settings.targets.get(1));
    }
    
    @Test(expected = BadUsageException.class)
    public void testBadUsageIfNoArgsGiven() {
        parseArgs(new String[0]);
    }
    
    @Test(expected = BadUsageException.class)
    public void testBadUsageIfNoTargetsGiven() {
        parseArgs("-p", "xoo.jar");
    }
    
    @Test
    public void testNoBadUsageIfOnlyHelpGiven() {
        parseArgs("-h");
    }
}
