package org.lockstepcheck.callgraph;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.Test;
import org.lockstepcheck.analysis.results.BasicCallGraphAnalysis;
import org.lockstepcheck.callgraph.CallGraph.CallSite;
import org.lockstepcheck.callgraph.CallGraph.ClassNode;
import org.lockstepcheck.callgraph.CallGraph.MethodNode;
import org.lockstepcheck.ignoreset.EmptyIgnoreSet;
import org.lockstepcheck.ignoreset.IgnoreSet;
import org.lockstepcheck.ignoreset.SimpleIgnoreSet;
import org.lockstepcheck.loaders.ClassFileLoader;
import org.lockstepcheck.loaders.DefaultClassFileLoader;
import org.lockstepcheck.misc.MethodPath;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodType;

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
        CallGraph cg = buildCg(new MethodPath(Simple.class, "one", "()I"));
        MethodNode one = cg.getClass(Simple.class).getMethod("one", mt("()I"));
        MethodNode two = cg.getClass(Simple.class).getMethod("two", mt("()I"));
        MethodNode three = cg.getClass(Simple.class).getMethod("three", mt("()I"));
        assertCall(one, two);
        assertCall(two, three);
        assertNoCall(two, one);
        assertNoCall(three, two);
        assertNoCall(three, one);
    }
    
    @Test
    public void testNonexistentRootMethods() {
        buildCg(new MethodPath(Simple.class, "one", "()V"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNonexistentRootClasses() throws Exception {
        ClassFileLoader loader = mock(ClassFileLoader.class);
        when(loader.loadClass(any(String.class))).thenThrow(new ClassNotFoundException());
        
        CallGraphBuilder builder = new CallGraphBuilder(loader);
        builder.addRoot(new Root("Nonexistent"));
        builder.getResult();
    }
    
    @Test
    public void testIgnoredRootClasses() throws Exception {
        ClassFileLoader loader = mock(ClassFileLoader.class);
        when(loader.loadClass(any(String.class))).thenReturn(null);
        SimpleIgnoreSet ignores = new SimpleIgnoreSet();
        ignores.addClass(Simple.class);
        
        CallGraphBuilder builder = new CallGraphBuilder(loader, ignores);
        builder.addRoot(new Root(Simple.class));
        
        assertFalse(builder.getResult().callGraph().hasClass(Simple.class));
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
        CallGraph cg = buildCg(new MethodPath(InternalMutualRecursion.class, "one", "(I)I"));
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
        
        public static void bar() {
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
        CallGraph cg = buildCg(new MethodPath(CyclicOne.class, "foo", "()V"));
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
    
    @Test
    public void testUsingRootClassesAnalyzesCorrectMethods() {
        BasicCallGraphAnalysis result1 = build(CyclicOne.class);
        BasicCallGraphAnalysis result2 = build(CyclicTwo.class);
        CallGraph cg1 = result1.callGraph();
        CallGraph cg2 = result2.callGraph();
        MethodNode cg1Foo = cg1.getClass(CyclicTwo.class).getMethod("foo", mt("()V"));
        MethodNode cg1Bar = cg1.getClass(CyclicTwo.class).getMethod("bar", mt("()V"));
        MethodNode cg2Foo = cg2.getClass(CyclicTwo.class).getMethod("foo", mt("()V"));
        MethodNode cg2Bar = cg2.getClass(CyclicTwo.class).getMethod("bar", mt("()V"));
        
        assertTrue(result1.basicAnalysisDoneMethods().contains(cg1Foo));
        assertFalse(result1.basicAnalysisDoneMethods().contains(cg1Bar));
        assertTrue(result2.basicAnalysisDoneMethods().contains(cg2Foo));
        assertTrue(result2.basicAnalysisDoneMethods().contains(cg2Bar));
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
        
        @Override
        public void foo() {
            super.foo();
        }
    }
    
    @Test
    public void testSuperclassConstructor() {
        CallGraph cg = buildCg(new MethodPath(Sub.class, "<init>", "(I)V"));
        MethodNode superCtor = cg.getClass(Super.class).getMethod("<init>", mt("(II)V"));
        MethodNode subCtor = cg.getClass(Sub.class).getMethod("<init>", mt("(I)V"));
        
        assertCall(subCtor, superCtor);
        assertNoCall(superCtor, subCtor);
    }
    
    @Test
    public void testPolymorphicCall() {
        CallGraph cg = buildCg(new MethodPath(Sub.class, "invokeFoo", "()V"));
        MethodNode superInvokeFoo = cg.getClass(Super.class).getMethod("invokeFoo", mt("()V"));
        MethodNode superFoo = cg.getClass(Super.class).getMethod("foo", mt("()V"));
        MethodNode subFoo = cg.getClass(Sub.class).getMethod("foo", mt("()V"));
        
        assertCall(superInvokeFoo, superFoo);
        assertNoCall(superInvokeFoo, subFoo);
    }
    
    @Test
    public void testSuperCallInMethod() {
        CallGraph cg = buildCg(new MethodPath(Sub.class, "foo", "()V"));
        MethodNode superFoo = cg.getClass(Super.class).getMethod("foo", mt("()V"));
        MethodNode subFoo = cg.getClass(Sub.class).getMethod("foo", mt("()V"));
        
        assertCall(subFoo, superFoo);
        assertNoCall(superFoo, subFoo);
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
        CallGraph cg = buildCg(new MethodPath(Outer.class, "outer", "()V"));
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
        BasicCallGraphAnalysis result = build(new MethodPath(Irrelevant.class, "noop", "()V"));
        CallGraph cg = result.callGraph();
        if (cg.hasClass(Simple.class)) {
            fail("The class Simple should not have been loaded into the call graph");
        }
        
        MethodNode noop = cg.getClass(Irrelevant.class).getMethod("noop", mt("()V"));
        MethodNode uninteresting = cg.getClass(Irrelevant.class).getMethod("uninteresting", mt("()I"));
        assertTrue(result.basicAnalysisDoneMethods().contains(noop));
        assertFalse(result.basicAnalysisDoneMethods().contains(uninteresting));
    }
    
    
    public static class PartlySfp {
        public strictfp double sfp(double x) {
            return 1.0 / x;
        }
        
        public double nonsfp(double x) {
            return 1.0 / x;
        }
        
        public double safe(double x) {
            return x;
        }
    }
    
    public static strictfp class FullySfp {
        public double foo(double x) {
            return x - 1.3;
        }
    }
    
    @Test
    public void testStrictFpDiscovery() {
        BasicCallGraphAnalysis result = build(
            new MethodPath(PartlySfp.class, "sfp", "(D)D"),
            new MethodPath(PartlySfp.class, "nonsfp", "(D)D"),
            new MethodPath(FullySfp.class, "foo", "(D)D")
        );
        CallGraph cg = result.callGraph();
        
        MethodNode sfp = cg.getClass(PartlySfp.class).getMethod("sfp", mt("(D)D"));
        MethodNode nonsfp = cg.getClass(PartlySfp.class).getMethod("nonsfp", mt("(D)D"));
        MethodNode safe = cg.getClass(PartlySfp.class).getMethod("safe", mt("(D)D"));
        MethodNode foo = cg.getClass(FullySfp.class).getMethod("foo", mt("(D)D"));
        
        assertTrue(result.localFpMathMethods().contains(sfp));
        assertTrue(result.localFpMathMethods().contains(nonsfp));
        assertFalse(result.localFpMathMethods().contains(safe));
        assertTrue(result.localFpMathMethods().contains(foo));
        
        assertTrue(result.strictfpMethods().contains(sfp));
        assertFalse(result.strictfpMethods().contains(nonsfp));
        assertFalse(result.strictfpMethods().contains(safe));
        assertTrue(result.strictfpMethods().contains(foo));
    }
    
    
    public static class WithNative {
        public native void foo();
        public void bar() {
        }
    }
    
    @Test
    public void testNativeMethodDiscovery() {
        BasicCallGraphAnalysis result = build(new MethodPath(WithNative.class, "foo", "()V"));
        CallGraph cg = result.callGraph();
        MethodNode foo = cg.getClass(WithNative.class).getMethod("foo", mt("()V"));
        MethodNode bar = cg.getClass(WithNative.class).getMethod("bar", mt("()V"));
        
        assertTrue(result.nativeMethods().contains(foo));
        assertFalse(result.nativeMethods().contains(bar));
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
        
        BasicCallGraphAnalysis result = build(ignores, new MethodPath(CallToIgnoredMethod.class, "foo", "()V"));
        CallGraph cg = result.callGraph();
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
        
        CallGraph cg = buildCg(ignores, new MethodPath(CallToIgnoredClass.class, "call", "()V"));
        MethodNode call = cg.getClass(CallToIgnoredClass.class).getMethod("call", mt("()V"));
        
        assertFalse(cg.hasClass(IgnoredClass.class));
        assertTrue(call.getOutgoingCalls().isEmpty());
    }
    
    @Test
    public void testIgnoredSuperclass() {
        SimpleIgnoreSet ignores = new SimpleIgnoreSet();
        ignores.addClass(Super.class);
        
        CallGraph cg = buildCg(ignores, new MethodPath(Sub.class, "foo", "()V"));
        assertFalse(cg.hasClass(Super.class));
        assertFalse(cg.getClass(Sub.class).hasMethod("invokeFoo", mt("()V")));
        assertTrue(cg.getClass(Sub.class).hasMethod("foo", mt("()V")));
        assertTrue(cg.getClass(Sub.class).getMethod("foo", mt("()V")).getIncomingCalls().isEmpty());
    }
    
    
    public interface FooInterface {
        public void foo();
    }
    
    public interface FoobarInterface extends FooInterface {
        public void bar();
    }
    
    public class FooInterfaceCaller {
        public void callFoo(FooInterface x) {
            x.foo();
        }
    }
    
    public abstract class FooAbstract implements FoobarInterface {
        public abstract void baz();
        public void callAll() {
            foo();
            bar();
            baz();
        }
    }
    
    @Test
    public void testDiscoveryOfSuperinterfaces() {
        CallGraph cg = buildCg(FooAbstract.class);
        
        assertTrue(cg.hasClass(FooInterface.class));
        assertTrue(cg.hasClass(FoobarInterface.class));
        
        List<ClassNode> hierarchy = cg.getClass(FooAbstract.class).getHierarchy();
        assertTrue(hierarchy.contains(cg.getClass(FooAbstract.class)));
        assertTrue(hierarchy.contains(cg.getClass(FoobarInterface.class)));
        assertTrue(hierarchy.contains(cg.getClass(FooInterface.class)));
    }
    
    @Test
    public void testMethodsInheritedFromInterface() {
        CallGraph cg = buildCg(FooAbstract.class);
        ClassNode cls = cg.getClass(FooAbstract.class);
        assertTrue(cls.hasMethod("foo", mt("()V")));
        assertTrue(cls.hasMethod("bar", mt("()V")));
    }
    
    @Test
    public void testCallsToInterfaceMethods() {
        String methodDesc = "(L" + FooInterface.class.getName().replace('.', '/') + ";)V";
        CallGraph cg = buildCg(new MethodPath(FooInterfaceCaller.class, "callFoo", methodDesc));
        
        MethodNode callFoo = cg.getClass(FooInterfaceCaller.class).getMethod("callFoo", mt(methodDesc));
        MethodNode foo = cg.getClass(FooInterface.class).getMethod("foo", mt("()V"));
        
        assertCall(callFoo, foo);
        assertNoCall(foo, callFoo);
    }
    
    @Test
    public void testCallsToAbstractMethods() {
        CallGraph cg = buildCg(new MethodPath(FooAbstract.class, "callAll", "()V"));
        
        MethodNode callAll = cg.getClass(FooAbstract.class).getMethod("callAll", mt("()V"));
        MethodNode foo = cg.getClass(FooAbstract.class).getMethod("foo", mt("()V"));
        MethodNode bar = cg.getClass(FooAbstract.class).getMethod("bar", mt("()V"));
        MethodNode baz = cg.getClass(FooAbstract.class).getMethod("baz", mt("()V"));
        
        assertCall(callAll, foo);
        assertCall(callAll, bar);
        assertCall(callAll, baz);
    }
    
    
    public class ArrayCaller {
        public void callMethodsOnArrays() {
            new String[0].toString();
            new String[0].clone(); // (clone is overloaded in array classes)
            new int[0][0].clone();
        }
    }
    
    @Test
    public void testNoAttemptToLoadArrayClasses() throws Exception {
        ClassFileLoader loader = mock(DefaultClassFileLoader.class);
        when(loader.loadClass(any(String.class))).thenAnswer(new Answer<ClassReader>() {
            @Override
            public ClassReader answer(InvocationOnMock invocation) throws Throwable {
                String arg = ((String)invocation.getArguments()[0]);
                if (arg.startsWith("[")) {
                    fail("Attempted to load array class " + arg);
                }
                return (ClassReader)invocation.callRealMethod();
            }
        });
        
        CallGraphBuilder builder = new CallGraphBuilder(loader);
        
        builder.addRoot(new Root(ArrayCaller.class));
        builder.getResult();
    }
    
    
    
    
    private CallGraph buildCg(MethodPath... methods) {
        return build(methods).callGraph();
    }
    
    private CallGraph buildCg(Class<?>... classes) {
        return build(classes).callGraph();
    }
    
    private CallGraph buildCg(IgnoreSet ignores, MethodPath... methods) {
        return build(ignores, methods).callGraph();
    }
    
    private BasicCallGraphAnalysis build(MethodPath... methods) {
        return build(new EmptyIgnoreSet(), methods);
    }
    
    private BasicCallGraphAnalysis build(Class<?>... classes) {
        return build(new EmptyIgnoreSet(), classes);
    }
    
    private BasicCallGraphAnalysis build(IgnoreSet ignores, MethodPath... methods) {
        try {
            CallGraphBuilder builder = new CallGraphBuilder(new DefaultClassFileLoader(), ignores);
            for (MethodPath method : methods) {
                builder.addRoot(new Root(method));
            }
            return builder.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private BasicCallGraphAnalysis build(IgnoreSet ignores, Class<?>... classes) {
        try {
            CallGraphBuilder builder = new CallGraphBuilder(new DefaultClassFileLoader(), ignores);
            for (Class<?> cls : classes) {
                builder.addRoot(new Root(cls));
            }
            return builder.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
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
