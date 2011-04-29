package org.portablejava.analysis;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodType;
import org.portablejava.analysis.StrictfpSafetyAnalyzer;
import org.portablejava.analysis.results.BasicCallGraphAnalysis;
import org.portablejava.analysis.results.CallPath;
import org.portablejava.analysis.results.StrictfpSafetyAnalysis;
import org.portablejava.callgraph.CallGraph;
import org.portablejava.callgraph.Root;
import org.portablejava.callgraph.CallGraph.ClassNode;
import org.portablejava.callgraph.CallGraph.MethodNode;

public class StrictfpSafetyAnalyzerTest {
    
    private final MethodType mt = new MethodType("()V");
    
    private CallGraph cg;
    private BasicCallGraphAnalysis basic;
    private StrictfpSafetyAnalyzer analyzer;

    @Before
    public void setUp() {
        cg = new CallGraph();
        basic = new BasicCallGraphAnalysis(cg);
        analyzer = new StrictfpSafetyAnalyzer(basic);
    }
    
    @Test
    public void testNoUnsafeMethod() {
        ClassNode a = cg.addClass("A", null);
        MethodNode m1 = a.addMethod("m1", mt);
        MethodNode m2 = a.addMethod("m2", mt);
        
        cg.addCall(m1, m2);
        cg.addCall(m2, m1);
        
        basic.localFpMathMethods().add(m2);
        basic.strictfpMethods().add(m2);
        
        analyzer.addRoot(new Root(m1.getPath()));
        StrictfpSafetyAnalysis result = analyzer.getResult();
        
        assertTrue(result.unsafeCallPaths().isEmpty());
    }
    
    @Test
    public void testAnalyzingUnsafeMethodDirectly() {
        ClassNode a = cg.addClass("A", null);
        MethodNode m1 = a.addMethod("m1", mt);
        
        basic.localFpMathMethods().add(m1);
        
        analyzer.addRoot(new Root(m1.getPath()));
        StrictfpSafetyAnalysis result = analyzer.getResult();
        
        assertEquals(CallPath.make(m1), result.unsafeCallPaths().get(m1));
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
        
        basic.localFpMathMethods().add(m2);
        basic.strictfpMethods().add(m2);
        basic.localFpMathMethods().add(unsafe);
        
        analyzer.addRoot(new Root(m1.getPath()));
        StrictfpSafetyAnalysis result = analyzer.getResult();
        
        assertEquals(CallPath.make(m1, m2, m3, unsafe), result.unsafeCallPaths().get(m1));
    }
    
}
