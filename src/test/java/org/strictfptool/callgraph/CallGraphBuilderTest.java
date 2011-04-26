package org.strictfptool.callgraph;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.objectweb.asm.MethodType;
import org.strictfptool.DefaultClassFileLoader;
import org.strictfptool.MethodPath;
import org.strictfptool.annotations.DoesLocalFpMath;
import org.strictfptool.annotations.NativeMethod;
import org.strictfptool.annotations.StrictfpMethod;
import org.strictfptool.annotations.TransitivelyAnalyzed;
import org.strictfptool.callgraph.CallGraph.CallSite;
import org.strictfptool.callgraph.CallGraph.MethodNode;
import org.strictfptool.ignoreset.EmptyIgnoreSet;
import org.strictfptool.ignoreset.IgnoreSet;
import org.strictfptool.ignoreset.SimpleIgnoreSet;

public class CallGraphBuilderTest {
    
    public static class Simple {
        public int one() {
            return two() + two();
        }
        
        public int two() {
            return three() - 1;
        }
        
        public int three() {
            return 3;
        }
    }
    
    @Test
    public void testNonrecursiveMethodCallsWithinOneClass() {
        CallGraph cg = build(new MethodPath(Simple.class, "one", "()I"));
        MethodNode one = cg.getClass(Simple.class).getMethod("one", mt("()I"));
        MethodNode two = cg.getClass(Simple.class).getMethod("two", mt("()I"));
        MethodNode three = cg.getClass(Simple.class).getMethod("three", mt("()I"));
        assertCall(one, two);
        assertCall(two, three);
        assertNoCall(two, one);
        assertNoCall(three, two);
        assertNoCall(three, one);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNonexistentRootMethods() {
        build(new MethodPath(Simple.class, "one", "()V"));
    }
    
    
    public static class InternalMutualRecursion {
        public int one(int x) {
            return two(x - 1);
        }
        
        public int two(int x) {
            if (x > 0) {
                return one(x);
            } else {
                return 0;
            }
        }
    }
    
    @Test
    public void testMutuallyRecursiveMethodsWithinOneClass() {
        CallGraph cg = build(new MethodPath(InternalMutualRecursion.class, "one", "(I)I"));
        MethodNode one = cg.getClass(InternalMutualRecursion.class).getMethod("one", mt("(I)I"));
        MethodNode two = cg.getClass(InternalMutualRecursion.class).getMethod("two", mt("(I)I"));
        assertCall(one, two);
        assertCall(two, one);
    }
    
    
    public static class CyclicOne {
        public void foo() {
            CyclicTwo.foo();
        }
    }
    
    public static class CyclicTwo {
        public static void foo() {
            new CyclicThree().foo();
        }
    }
    
    public static class CyclicThree {
        public void foo() {
            new CyclicOne().foo();
        }
    }
    
    @Test
    public void testMutuallyRecursiveMethodsAcrossClasses() {
        CallGraph cg = build(new MethodPath(CyclicOne.class, "foo", "()V"));
        MethodNode one = cg.getClass(CyclicOne.class).getMethod("foo", mt("()V"));
        MethodNode two = cg.getClass(CyclicTwo.class).getMethod("foo", mt("()V"));
        MethodNode three = cg.getClass(CyclicThree.class).getMethod("foo", mt("()V"));
        assertCall(one, two);
        assertCall(two, three);
        assertCall(three, one);
        assertNoCall(two, one);
        assertNoCall(three, two);
        assertNoCall(one, three);
    }
    
    
    public static class Super {
        public Super(int x, int y) {
        }
        
        public void invokeFoo() {
            foo();
        }
        
        public void foo() {
        }
    }
    
    public static class Sub extends Super {
        public Sub(int x) {
            super(x, x);
        }
        
        public void foo() {
        }
    }
    
    @Test
    public void testSuperclassConstructor() {
        CallGraph cg = build(new MethodPath(Sub.class, "<init>", "(I)V"));
        MethodNode superCtor = cg.getClass(Super.class).getMethod("<init>", mt("(II)V"));
        MethodNode subCtor = cg.getClass(Sub.class).getMethod("<init>", mt("(I)V"));
        assertCall(subCtor, superCtor);
        assertNoCall(superCtor, subCtor);
    }
    
    @Test
    public void testPolymorphicCall() {
        CallGraph cg = build(new MethodPath(Sub.class, "invokeFoo", "()V"));
        MethodNode superInvokeFoo = cg.getClass(Super.class).getMethod("invokeFoo", mt("()V"));
        MethodNode superFoo = cg.getClass(Super.class).getMethod("foo", mt("()V"));
        MethodNode subFoo = cg.getClass(Sub.class).getMethod("foo", mt("()V"));
        assertCall(superInvokeFoo, superFoo);
        assertNoCall(superInvokeFoo, subFoo);
    }
    
    
    public static class Outer {
        public void outer() {
            (new Object() {
                public void anon() {
                    new Inner().inner();
                }
            }).anon();
        }
        public class Inner {
            public void inner() {
            }
        }
    }
    
    @Test
    public void testNestedClasses() {
        CallGraph cg = build(new MethodPath(Outer.class, "outer", "()V"));
        MethodNode outer = cg.getClass(Outer.class).getMethod("outer", mt("()V"));
        MethodNode inner = cg.getClass(Outer.Inner.class).getMethod("inner", mt("()V"));
        
        MethodNode anon = requireCall(outer, "anon");
        assertCall(anon, inner);
    }

    
    public static class Irrelevant {
        public void noop() {
        }
        
        public int uninteresting() {
            return new Simple().one();
        }
    }
    
    @Test
    public void testNotDoingTooMuchAnalysis() {
        CallGraph cg = build(new MethodPath(Irrelevant.class, "noop", "()V"));
        if (cg.hasClass(Simple.class)) {
            fail("The class Simple should not have been loaded into the call graph");
        }
        
        MethodNode noop = cg.getClass(Irrelevant.class).getMethod("noop", mt("()V"));
        MethodNode uninteresting = cg.getClass(Irrelevant.class).getMethod("uninteresting", mt("()I"));
        assertTrue(noop.hasAnnotation(TransitivelyAnalyzed.class));
        assertFalse(uninteresting.hasAnnotation(TransitivelyAnalyzed.class));
    }
    
    
    public static class PartlySfp {
        public strictfp double sfp(double x) {
            return 1.0 / x;
        }
        
        public double nonsfp(double x) {
            return 1.0 / x;
        }
    }
    
    public static strictfp class FullySfp {
        public double foo(double x) {
            return x - 1.3;
        }
    }
    
    @Test
    public void testStrictFpDiscovery() {
        CallGraph cg = build(
            new MethodPath(PartlySfp.class, "sfp", "(D)D"),
            new MethodPath(PartlySfp.class, "nonsfp", "(D)D"),
            new MethodPath(FullySfp.class, "foo", "(D)D")
        );
        
        MethodNode sfp = cg.getClass(PartlySfp.class).getMethod("sfp", mt("(D)D"));
        MethodNode nonsfp = cg.getClass(PartlySfp.class).getMethod("nonsfp", mt("(D)D"));
        MethodNode foo = cg.getClass(FullySfp.class).getMethod("foo", mt("(D)D"));
        
        assertTrue(sfp.hasAnnotation(DoesLocalFpMath.class));
        assertTrue(nonsfp.hasAnnotation(DoesLocalFpMath.class));
        assertTrue(foo.hasAnnotation(DoesLocalFpMath.class));
        
        assertTrue(sfp.hasAnnotation(StrictfpMethod.class));
        assertFalse(nonsfp.hasAnnotation(StrictfpMethod.class));
        assertTrue(foo.hasAnnotation(StrictfpMethod.class));
    }
    
    
    public static class WithNative {
        public native void foo();
        public void bar() {
        }
    }
    
    @Test
    public void testNativeMethodDiscovery() {
        CallGraph cg = build(new MethodPath(WithNative.class, "foo", "()V"));
        MethodNode foo = cg.getClass(WithNative.class).getMethod("foo", mt("()V"));
        MethodNode bar = cg.getClass(WithNative.class).getMethod("bar", mt("()V"));
        assertTrue(foo.hasAnnotation(NativeMethod.class));
        assertFalse(bar.hasAnnotation(NativeMethod.class));
    }
    
    
    public static class CallToIgnoredMethod {
        public void foo() {
            uninteresting();
        }
        
        public void uninteresting() {
            new Simple().one();
        }
    }
    
    @Test
    public void testIgnoredMethods() {
        SimpleIgnoreSet ignores = new SimpleIgnoreSet();
        ignores.addMethod(new MethodPath(CallToIgnoredMethod.class, "uninteresting", "()V"));
        
        CallGraph cg = build(ignores, new MethodPath(CallToIgnoredMethod.class, "foo", "()V"));
        MethodNode foo = cg.getClass(CallToIgnoredMethod.class).getMethod("foo", mt("()V"));
        assertTrue(foo.getOutgoingCalls().isEmpty());
        assertFalse(cg.getClass(CallToIgnoredMethod.class).hasMethod("uninteresting", mt("()V")));
        assertFalse(cg.hasClass(Simple.class));
    }
    
    
    public static class CallToIgnoredClass {
        public static void call() {
            IgnoredClass.ignored();
        }
    }
    
    public static class IgnoredClass {
        public static void ignored() {
        }
    }
    
    @Test
    public void testIgnoredClasses() {
        SimpleIgnoreSet ignores = new SimpleIgnoreSet();
        ignores.addClass(IgnoredClass.class);
        
        CallGraph cg = build(ignores, new MethodPath(CallToIgnoredClass.class, "call", "()V"));
        MethodNode call = cg.getClass(CallToIgnoredClass.class).getMethod("call", mt("()V"));
        
        assertFalse(cg.hasClass(IgnoredClass.class));
        assertTrue(call.getOutgoingCalls().isEmpty());
    }
    
    @Test
    public void testIgnoredSuperclass() {
        SimpleIgnoreSet ignores = new SimpleIgnoreSet();
        ignores.addClass(Super.class);
        
        CallGraph cg = build(ignores, new MethodPath(Sub.class, "foo", "()V"));
        assertFalse(cg.hasClass(Super.class));
        assertFalse(cg.getClass(Sub.class).hasMethod("invokeFoo", mt("()V")));
        assertTrue(cg.getClass(Sub.class).hasMethod("foo", mt("()V")));
        assertTrue(cg.getClass(Sub.class).getMethod("foo", mt("()V")).getIncomingCalls().isEmpty());
    }
    
    
    
    private CallGraph build(MethodPath... methods) {
        return build(EmptyIgnoreSet.getInstance(), methods);
    }
    
    private CallGraph build(IgnoreSet ignores, MethodPath... methods) {
        Set<MethodPath> methodSet = new HashSet<MethodPath>(Arrays.asList(methods));
        try {
            return CallGraphBuilder.buildCallGraph(new DefaultClassFileLoader(), methodSet, ignores);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private MethodType mt(String desc) {
        return new MethodType(desc);
    }
    
    private void assertCall(MethodNode one, MethodNode two) {
        if (!existsCall(one, two)) {
            fail("No call from " + one + " to " + two);
        }
    }
    
    private void assertNoCall(MethodNode one, MethodNode two) {
        if (existsCall(one, two)) {
            fail("Found call from " + one + " to " + two + " that's not supposed to be there");
        }
    }
    
    private boolean existsCall(MethodNode one, MethodNode two) {
        for (CallSite cs : one.getOutgoingCalls()) {
            if (cs.getFrom() == one) {
                for (CallSite cs2 : two.getIncomingCalls()) {
                    if (cs2 == cs) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private MethodNode requireCall(MethodNode outer, String targetMethodName) {
        for (CallSite cs : outer.getOutgoingCalls()) {
            if (cs.getTo().getName().equals(targetMethodName)) {
                return cs.getTo();
            }
        }
        fail("No call from " + outer + " to any method named " + targetMethodName);
        throw new IllegalStateException();
    }
}
