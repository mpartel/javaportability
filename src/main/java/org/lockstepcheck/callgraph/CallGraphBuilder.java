package org.lockstepcheck.callgraph;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

import org.lockstepcheck.analysis.results.BasicCallGraphAnalysis;
import org.lockstepcheck.callgraph.CallGraph.ClassNode;
import org.lockstepcheck.callgraph.CallGraph.MethodNode;
import org.lockstepcheck.ignoreset.IgnoreSet;
import org.lockstepcheck.loaders.ClassFileLoader;
import org.lockstepcheck.misc.CheckedExceptionWrapper;
import org.lockstepcheck.misc.MethodPath;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

public class CallGraphBuilder extends EmptyVisitor {

    private boolean traceEnabled;
    private CallGraph callGraph;
    private BasicCallGraphAnalysis result;
    private ClassFileLoader classFileLoader;
    private IgnoreSet ignoreSet;
    private Queue<String> classDiscoveryQueue;
    private Queue<MethodPath> methodQueue;
    private HashMap<MethodNode, List<MethodPath>> unanalyzedCalls;
    
    private static class Root {
        private static final Pattern ACCEPT_ALL = Pattern.compile(".*");
        private final String className;
        private final Pattern methodPattern;
        private final Pattern methodDescPattern;
        
        public Root(String className) {
            this(className, ACCEPT_ALL);
        }
        
        public Root(String className, String methodName) {
            this(className, exactMatcher(methodName));
        }

        public Root(MethodPath methodPath) {
            this(methodPath.getOwner(), exactMatcher(methodPath.getName()), exactMatcher(methodPath.getDesc()));
        }
        
        public Root(String className, Pattern methodPattern) {
            this(className, methodPattern, ACCEPT_ALL);
        }
        
        public Root(String className, Pattern methodPattern, Pattern methodDescPattern) {
            this.className = className;
            this.methodPattern = methodPattern;
            this.methodDescPattern = methodDescPattern;
        }
        
        private static Pattern exactMatcher(String methodName) {
            return Pattern.compile(Pattern.quote(methodName));
        }
        
        public String getClassName() {
            return className;
        }
        
        public Pattern getMethodPattern() {
            return methodPattern;
        }
        
        public Pattern getMethodDescPattern() {
            return methodDescPattern;
        }
    }
    
    public CallGraphBuilder(ClassFileLoader classFileLoader, IgnoreSet ignoreSet) {
        this.callGraph = new CallGraph();
        this.result = new BasicCallGraphAnalysis(callGraph);
        this.classFileLoader = classFileLoader;
        this.ignoreSet = ignoreSet;
        this.classDiscoveryQueue = new LinkedList<String>();
        this.methodQueue = new LinkedList<MethodPath>();
        this.unanalyzedCalls = new HashMap<MethodNode, List<MethodPath>>();
    }
    
    public void setDebugTrace(boolean enabled) {
        traceEnabled = enabled;
    }
    
    public void addRootClass(String className) throws Exception {
        addRoot(new Root(className));
    }
    
    public void addRootClass(Class<?> cls) throws Exception {
        addRootClass(cls.getName().replace('.', '/'));
    }
    
    public void addRootMethod(MethodPath methodPath) throws Exception {
        addRoot(new Root(methodPath));
    }
    
    public void addRootMethod(String className, String methodName) throws Exception {
        addRoot(new Root(className, methodName));
    }
    
    public void addRootMethods(String className, Pattern methodPattern) throws Exception {
        addRoot(new Root(className, methodPattern));
    }
    
    private void addRoot(Root root) throws Exception {
        String className = root.getClassName();
        try {
            if (!ignoreSet.containsClass(className)) {
                if (classNotYetDiscovered(className)) {
                    discoverClass(className);
                }
                enqueueMethodsInRoot(root);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found: " + className, e);
        }
        
        mainLoop();
    }
    
    public BasicCallGraphAnalysis getResult() {
        return result;
    }
    
    private void mainLoop() throws ClassNotFoundException, IOException {
        try {
            while (true) {
                if (!classDiscoveryQueue.isEmpty()) {
                    workClassDiscoveryQueue();
                } else if (!methodQueue.isEmpty()) {
                    workMethodQueue();
                } else {
                    break;
                }
            }
        } catch (CheckedExceptionWrapper e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                throw (ClassNotFoundException)e.getCause();
            } else if (e.getCause() instanceof IOException) {
                throw (IOException)e.getCause();
            }
        }
    }

    private void workClassDiscoveryQueue() throws ClassNotFoundException, IOException {
        String internalName = classDiscoveryQueue.remove();
        if (!callGraph.hasClass(internalName)) {
            discoverClass(internalName);
        }
    }
    
    private void workMethodQueue() {
        MethodPath m = methodQueue.peek();
        trace("Trying to process " + m);
        if (tryProcessMethod(m)) {
            methodQueue.remove();
        }
    }
    
    private boolean classNotYetDiscovered(String className) {
        return !callGraph.hasClass(className) && !ignoreSet.containsClass(className);
    }
    
    private void enqueueMethodsInRoot(Root root) {
        for (MethodNode method : callGraph.getClass(root.getClassName()).getMethodsIncludingInherited()) {
            boolean nameMatches = root.getMethodPattern().matcher(method.getName()).matches();
            boolean descMatches = root.getMethodDescPattern().matcher(method.getDesc()).matches();
            if (nameMatches && descMatches) {
                methodQueue.add(method.getPath());
                trace("Enqueued method " + method);
            }
        }
    }
    
    private ClassNode discoverClass(String internalName) throws ClassNotFoundException, IOException {
        if (!isBasicArrayClass(internalName) && !ignoreSet.containsClass(internalName)) {
            trace("Discovering class " + internalName);
            
            ClassReader reader = classFileLoader.loadClass(internalName);
            ClassDiscoverer discoverer = new ClassDiscoverer();
            reader.accept(discoverer, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return discoverer.getClassNode();
        } else {
            return null;
        }
    }
    
    private static final Pattern arrayClassRegex = Pattern.compile("^\\[+.$");
    
    private boolean isBasicArrayClass(String internalName) {
        return arrayClassRegex.matcher(internalName).matches();
    }
    
    private boolean tryProcessMethod(MethodPath methodPath) {
        if (!callGraph.hasClass(methodPath.getOwner())) {
            trace("Class owning method " + methodPath + " not yet discovered");
            classDiscoveryQueue.add(methodPath.getOwner());
            return false;
        }
        
        MethodNode methodNode = getMethodNode(methodPath);
        
        if (result.basicAnalysisDoneMethods().contains(methodNode)) {
            trace("Already analyzed method " + methodPath);
            return true;
        }
        
        if (enqueueUndiscoveredCalleeClasses(methodNode) == 0) {
            processCallsFromMethod(methodNode);
            return true;
        } else {
            trace("Undiscovered callee classes for " + methodPath);
            return false;
        }
    }
    
    private MethodNode getMethodNode(MethodPath methodPath) {
        return callGraph.getClass(methodPath.getOwner()).getMethod(methodPath.getName(), new MethodType(methodPath.getDesc()));
    }

    private int enqueueUndiscoveredCalleeClasses(MethodNode methodNode) {
        List<MethodPath> calls = unanalyzedCalls.get(methodNode);
        if (calls == null) {
            return 0;
        }
        
        int count = 0;
        for (MethodPath callee : calls) {
            if (classNotYetDiscovered(callee.getOwner())) {
                classDiscoveryQueue.add(callee.getOwner());
                count += 1;
            }
        }
        
        return count;
    }
    
    private void processCallsFromMethod(MethodNode methodNode) {
        result.basicAnalysisDoneMethods().add(methodNode);
        
        List<MethodPath> calls = unanalyzedCalls.remove(methodNode);
        
        for (MethodPath callee : calls) {
            if (ignoreSet.containsMethod(callee)) {
                continue;
            }
            
            MethodNode calleeNode = getMethodNode(callee);
            callGraph.addCall(methodNode, calleeNode);
            
            if (!result.basicAnalysisDoneMethods().contains(calleeNode)) {
                methodQueue.add(callee);
            }
            
            trace("Recorded call from " + methodNode + " to " + calleeNode);
        }
    }

    private class ClassDiscoverer extends EmptyVisitor {
        
        private ClassNode cls;
        
        public ClassDiscoverer() {
            this.cls = null;
        }

        public ClassNode getClassNode() {
            return cls;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            ClassNode superNode = getSuperclassNode(superName);
            cls = callGraph.addClass(name, superNode);
        }
        
        private ClassNode getSuperclassNode(String superName) {
            ClassNode superNode = null;
            if (superName != null) {
                if (callGraph.hasClass(superName)) {
                    superNode = callGraph.getClass(superName);
                } else {
                    try {
                        superNode = discoverClass(superName);
                    } catch (ClassNotFoundException e) {
                        throw new CheckedExceptionWrapper(e);
                    } catch (IOException e) {
                        throw new CheckedExceptionWrapper(e);
                    }
                }
            }
            return superNode;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodPath path = new MethodPath(cls.getName(), name, desc);
            if (ignoreSet.containsMethod(path)) {
                trace("Ignored method " + path);
                return new EmptyVisitor();
            }
            
            trace("Discovered method " + path);
            
            MethodNode method = cls.addMethod(name, new MethodType(desc));
            if (isStrictfp(access)) {
                result.strictfpMethods().add(method);
            }
            if (isNative(access)) {
                result.nativeMethods().add(method);
            }
            
            return new MethodDiscoverer(method);
        }
        
        private boolean isStrictfp(int access) {
            return (access & ACC_STRICT) != 0;
        }
        
        private boolean isNative(int access) {
            return (access & ACC_NATIVE) != 0;
        }
    }
    
    
    private class MethodDiscoverer extends EmptyVisitor {
        
        private MethodNode methodNode;
        private boolean foundFpMath;
        private List<MethodPath> unanalyzedCallsForThis;
        
        public MethodDiscoverer(MethodNode methodNode) {
            this.methodNode = methodNode;
            this.foundFpMath = false;
            unanalyzedCallsForThis = new LinkedList<MethodPath>();
            unanalyzedCalls.put(methodNode, unanalyzedCallsForThis);
        }
        
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            trace("Found call from " + methodNode + "  to  " + owner + " " + name + " " + desc);
            MethodPath to = new MethodPath(owner, name, desc);
            unanalyzedCallsForThis.add(to);
        }

        @Override
        public void visitInsn(int opcode) {
            if (!foundFpMath && isFloatArithmetic(opcode)) {
                result.localFpMathMethods().add(methodNode);
                foundFpMath = true;
            }
        }
        
        private boolean isFloatArithmetic(int opcode) {
            switch (opcode) {
            case DADD:
            case FADD:
            case DSUB:
            case FSUB:
            case DMUL:
            case FMUL:
            case DDIV:
            case FDIV:
            case DREM:
            case FREM:
            case DNEG:
            case FNEG:
            case I2D:
            case I2F:
            case L2D:
            case L2F:
            case F2I:
            case F2L:
            case F2D:
            case D2I:
            case D2L:
            case D2F:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                return true;
            default:
                return false;
            }
        }
    }
    
    private final void trace(String msg) {
        if (traceEnabled) {
            System.out.println(msg);
        }
    }
}
