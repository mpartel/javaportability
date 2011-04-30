package org.javaportability.analysis;

import static org.junit.Assert.*;

import org.javaportability.analysis.AnalysisSettings;
import org.javaportability.analysis.StrictfpSafetyAnalyzer;
import org.javaportability.analysis.results.BasicCallGraphAnalysis;
import org.javaportability.analysis.results.CallPath;
import org.javaportability.analysis.results.StrictfpSafetyAnalysis;
import org.javaportability.callgraph.CallGraph;
import org.javaportability.callgraph.Root;
import org.javaportability.callgraph.CallGraph.ClassNode;
import org.javaportability.callgraph.CallGraph.MethodNode;
import org.javaportability.callgraph.nodeset.SimpleNodeSet;
import org.javaportability.loaders.DefaultClassFileLoader;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodType;

public class StrictfpSafetyAnalyzerTest {
    
    private final MethodType mt = new MethodType("()V");
    
    private CallGraph cg;
    private AnalysisSettings settings;
    private BasicCallGraphAnalysis basic;
    private StrictfpSafetyAnalyzer analyzer;


    @Before
    public void setUp() {
        cg = new CallGraph();
        settings = new AnalysisSettings(new DefaultClassFileLoader());
        basic = new BasicCallGraphAnalysis(settings, cg);
        analyzer = new StrictfpSafetyAnalyzer(basic);
    }
    
    @Test
    public void testNoUnsafeMethod() {
        ClassNode a = cg.addClass("A", null);
        MethodNode m1 = a.addMethod("m1", mt);
        MethodNode m2 = a.addMethod("m2", mt);
        
        cg.addCall(m1, m2);
        cg.addCall(m2, m1);
        
        basic.localFpMathMethods.add(m2);
        basic.strictfpMethods.add(m2);
        
        StrictfpSafetyAnalysis result = analyzeFrom(m1);
        
        assertTrue(result.unsafeCallPaths.isEmpty());
    }
    
    @Test
    public void testAnalyzingUnsafeMethodDirectly() {
        ClassNode a = cg.addClass("A", null);
        MethodNode m1 = a.addMethod("m1", mt);
        
        basic.localFpMathMethods.add(m1);
        
        StrictfpSafetyAnalysis result = analyzeFrom(m1);
        
        assertEquals(CallPath.make(m1), result.unsafeCallPaths.get(m1));
    }
    
    @Test
    public void testCyclicCallsInvolvingUnsafeMethod() {
        ClassNode a = cg.addClass("A", null);
        MethodNode m1 = a.addMethod("m1", mt);
        MethodNode m2 = a.addMethod("m2", mt);
        MethodNode m3 = a.addMethod("m3", mt);
        MethodNode unsafe = a.addMethod("unsafe", mt);
        
        cg.addCall(m1, m2);
        cg.addCall(m2, m3);
        cg.addCall(m3, m1);
        cg.addCall(m3, unsafe);
        
        basic.localFpMathMethods.add(m2);
        basic.strictfpMethods.add(m2);
        basic.localFpMathMethods.add(unsafe);
        
        StrictfpSafetyAnalysis result = analyzeFrom(m1);
        
        assertEquals(CallPath.make(m1, m2, m3, unsafe), result.unsafeCallPaths.get(m1));
    }
    
    @Test
    public void testMethodsOnFpmathWhitelistAreSafeDespiteFpMath() {
        ClassNode a = cg.addClass("A", null);
        MethodNode m1 = a.addMethod("m1", mt);
        
        settings.allowedFpMath = new SimpleNodeSet();
        ((SimpleNodeSet)settings.allowedFpMath).addMethod(m1.getPath());
        basic.localFpMathMethods.add(m1);
        
        StrictfpSafetyAnalysis result = analyzeFrom(m1);
        
        assertNull(result.unsafeCallPaths.get(m1));
    }
    
    @Test
    public void testCallPathsAcrossMethodMarkedInherentlySafeAreAlwaysSafe() {
        ClassNode a = cg.addClass("A", null);
        MethodNode root = a.addMethod("root", mt);
        MethodNode safe = a.addMethod("safe", mt);
        MethodNode unsafe = a.addMethod("unsafe", mt);
        cg.addCall(root, safe);
        cg.addCall(safe, unsafe);
        
        settings.assumedSafe = new SimpleNodeSet();
        ((SimpleNodeSet)settings.assumedSafe).addMethod(safe.getPath());
        basic.localFpMathMethods.add(unsafe);
        
        StrictfpSafetyAnalysis result = analyzeFrom(root);
        
        assertNull(result.unsafeCallPaths.get(root));
    }
    
    @Test
    public void testCallPathsGoingPastAMethodAssumedSafeMayBeUnsafe() {
        ClassNode a = cg.addClass("A", null);
        MethodNode root = a.addMethod("root", mt);
        MethodNode safe = a.addMethod("safe", mt);
        MethodNode sidestep = a.addMethod("sidestep", mt);
        MethodNode unsafe = a.addMethod("unsafe", mt);
        cg.addCall(root, safe);
        cg.addCall(safe, unsafe);
        cg.addCall(root, sidestep);
        cg.addCall(sidestep, unsafe);
        
        settings.assumedSafe = new SimpleNodeSet();
        ((SimpleNodeSet)settings.assumedSafe).addMethod(safe.getPath());
        basic.localFpMathMethods.add(unsafe);
        
        StrictfpSafetyAnalysis result = analyzeFrom(root);
        
        assertEquals(CallPath.make(root, sidestep, unsafe), result.unsafeCallPaths.get(root));
    }
    
    @Test
    public void testMethodAssumedSafeIsSafeEvenIfItDoesUnsafeMath() {
        ClassNode a = cg.addClass("A", null);
        MethodNode m1 = a.addMethod("m1", mt);
        MethodNode m2 = a.addMethod("m2", mt);
        MethodNode m3 = a.addMethod("m3", mt);
        cg.addCall(m1, m2);
        cg.addCall(m2, m3);
        
        settings.assumedSafe = new SimpleNodeSet();
        ((SimpleNodeSet)settings.assumedSafe).addMethod(m2.getPath());
        basic.localFpMathMethods.add(m3);
        
        assertNull(analyzeFrom(m1).unsafeCallPaths.get(m1));
    }
    
    @Test
    public void testMethodAssumedUnsafeAreAlwaysUnsafe() {
        ClassNode a = cg.addClass("A", null);
        MethodNode m1 = a.addMethod("m1", mt);
        MethodNode m2 = a.addMethod("m2", mt);
        cg.addCall(m1, m2);
        
        settings.assumedUnsafe = new SimpleNodeSet();
        ((SimpleNodeSet)settings.assumedUnsafe).addMethod(m2.getPath());
        
        StrictfpSafetyAnalysis result = analyzeFrom(m1);
        
        assertEquals(CallPath.make(m1, m2), result.unsafeCallPaths.get(m1));
    }
    
    //TODO: assumedUnsafe blacklist should be prioritized
    
    

    private StrictfpSafetyAnalysis analyzeFrom(MethodNode root) {
        analyzer.addRoot(new Root(root.getPath()));
        return analyzer.getResult();
    }
}
