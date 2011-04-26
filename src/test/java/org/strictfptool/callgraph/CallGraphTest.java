package org.strictfptool.callgraph;

import static org.junit.Assert.*;

import org.junit.Test;
import org.objectweb.asm.MethodType;
import org.strictfptool.callgraph.CallGraph.CallSite;
import org.strictfptool.callgraph.CallGraph.ClassNode;
import org.strictfptool.callgraph.CallGraph.MethodNode;

public class CallGraphTest {
    
    @Test
    public void testFindingCallersAndCallees() {
        CallGraph cg = new CallGraph();
        ClassNode a = cg.addClass("A");
        ClassNode b = cg.addClass("B");
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
}
