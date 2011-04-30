package org.javaportability.app;

import static org.junit.Assert.*;

import java.util.List;

import org.javaportability.app.ArgParser;
import org.javaportability.app.BadUsageException;
import org.javaportability.app.Settings;
import org.junit.Test;

public class ArgParserTest {
    
    private Settings parseArgs(String... args) {
        return new ArgParser().parseArgs(args);
    }
    
    private Settings parseVerifyArgs(String... args) {
        String[] argsWithVerify = new String[args.length + 1];
        argsWithVerify[0] = "verify";
        System.arraycopy(args, 0, argsWithVerify, 1, args.length);
        return parseArgs(argsWithVerify);
    }
    
    @Test
    public void testHelp() {
        assertTrue(parseVerifyArgs("-h").help);
        assertTrue(parseVerifyArgs("--help").help);
    }
    
    @Test
    public void testDebug() {
        assertTrue(parseVerifyArgs("--debug", "Target").trace);
        assertFalse(parseVerifyArgs("Target").trace);
    }
    
    @Test
    public void testVerbose() {
        assertTrue(parseVerifyArgs("--verbose", "Target").verbose);
        assertTrue(parseVerifyArgs("-v", "Target").verbose);
        assertFalse(parseVerifyArgs("Target").verbose);
    }
    
    @Test
    public void testSearchPath() {
        List<String> sp = parseVerifyArgs("-p", "foo/foo/:bar.jar", "Target").searchPath;
        assertEquals(2, sp.size());
        assertEquals("foo/foo/", sp.get(0));
        assertEquals("bar.jar", sp.get(1));
    }
    
    @Test
    public void testCombined() {
        Settings settings = parseVerifyArgs("--path", "xoo/foo.jar", "One", "Two::method");
        assertEquals(1, settings.searchPath.size());
        assertEquals("xoo/foo.jar", settings.searchPath.get(0));
        assertEquals("One", settings.targets.get(0));
        assertEquals("Two::method", settings.targets.get(1));
    }
    
    @Test(expected = BadUsageException.class)
    public void testBadUsageIfSearchPathEntryMissingTrailingSlashAndJarExtension() {
        parseVerifyArgs("-p", "foo/foo:bar.jar", "Target");
    }
    
    @Test(expected = BadUsageException.class)
    public void testBadUsageIfOnlyVerifyCommandGiven() {
        parseVerifyArgs(new String[0]);
    }
    
    @Test(expected = BadUsageException.class)
    public void testBadUsageIfNoTargetsGiven() {
        parseVerifyArgs("-p", "xoo.jar");
    }
    
    @Test(expected = BadUsageException.class)
    public void testBadUsageIfInvalidCommandGiven() {
        try {
            parseArgs("oops");
        } catch (BadUsageException e) {
            assertEquals("Invalid command 'oops'", e.getMessage());
            throw e;
        }
    }
    
    @Test
    public void testNoBadUsageIfOnlyHelpGiven() {
        parseArgs("-h");
    }
}
