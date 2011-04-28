package org.lockstepcheck.callgraph;

import static org.junit.Assert.*;

import org.junit.Test;
import org.lockstepcheck.callgraph.CallGraph.CallSite;
import org.lockstepcheck.callgraph.CallGraph.ClassNode;
import org.lockstepcheck.callgraph.CallGraph.MethodNode;
import org.objectweb.asm.MethodType;

public class CallGraphTest {
    
    @Test
    public void testFindingCallersAndCallees() {
        CallGraph cg = new CallGraph();
        ClassNode a = cg.addClass("A", null);
        ClassNode b = cg.addClass("B", null);
        MethodNode foo = a.addMethod("foo", new MethodType("()V"));
        MethodNode bar = b.addMethod("bar", new MethodType("()V"));
        cg.addCall(foo, bar);
        
        assertEquals(1, foo.getOutgoingCalls().size());
        assertEquals(0, foo.getIncomingCalls().size());
        assertEquals(0, bar.getOutgoingCalls().size());
        assertEquals(1, bar.getIncomingCalls().size());
        assertSame(bar.getIncomingCalls().get(0), foo.getOutgoingCalls().get(0));
        
        CallSite cs = foo.getOutgoingCalls().get(0);
        assertSame(foo, cs.getFrom());
        assertSame(bar, cs.getTo());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAddingClassTwice() {
        CallGraph cg = new CallGraph();
        cg.addClass("A", null);
        cg.addClass("A", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddingMethodTwice() {
        CallGraph cg = new CallGraph();
        ClassNode a = cg.addClass("A", null);
        a.addMethod("foo", new MethodType("()V"));
        a.addMethod("foo", new MethodType("()V"));
    }
    
    @Test
    public void testAddingOverloadedMethod() {
        CallGraph cg = new CallGraph();
        ClassNode a = cg.addClass("A", null);
        a.addMethod("foo", new MethodType("()V"));
        a.addMethod("foo", new MethodType("()I"));
        assertEquals(2, a.getLocalMethods().size());
    }
    
    @Test
    public void testFindingMethodsDefinedInSuperclass() {
        CallGraph cg = new CallGraph();
        ClassNode a = cg.addClass("A", null);
        ClassNode b = cg.addClass("B", a);
        MethodNode m = a.addMethod("foo", new MethodType("()V"));
        
        assertSame(m, b.getMethod("foo", new MethodType("()V")));
        
        assertEquals(1, b.getMethodsIncludingInherited().size());
        assertSame(m, b.getMethodsIncludingInherited().get(0));
    }
    
    @Test
    public void testOverridingMethods() {
        CallGraph cg = new CallGraph();
        ClassNode a = cg.addClass("A", null);
        ClassNode b = cg.addClass("B", a);
        MethodNode m1 = a.addMethod("foo", new MethodType("()V"));
        MethodNode m2 = b.addMethod("foo", new MethodType("()V"));
        assertSame(m1, a.getMethod("foo", new MethodType("()V")));
        assertSame(m2, b.getMethod("foo", new MethodType("()V")));
        
        assertEquals(2, b.getMethodsIncludingInherited().size());
        assertTrue(b.getMethodsIncludingInherited().contains(m1));
        assertTrue(b.getMethodsIncludingInherited().contains(m2));
    }
    
}
