package org.portablejava.callgraph;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.EmptyVisitor;
import org.portablejava.analysis.results.BasicCallGraphAnalysis;
import org.portablejava.callgraph.CallGraph.ClassNode;
import org.portablejava.callgraph.CallGraph.MethodNode;
import org.portablejava.callgraph.nodeset.EmptyNodeSet;
import org.portablejava.callgraph.nodeset.NodeSet;
import org.portablejava.loaders.ClassFileLoader;
import org.portablejava.misc.CheckedExceptionWrapper;
import org.portablejava.misc.MethodPath;

public class CallGraphBuilder extends EmptyVisitor {

    private boolean traceEnabled;
    private CallGraph callGraph;
    private BasicCallGraphAnalysis result;
    private ClassFileLoader classFileLoader;
    private NodeSet ignoreSet;
    private Queue<String> classDiscoveryQueue;
    private Queue<MethodPath> methodQueue;
    private HashMap<MethodNode, List<MethodPath>> unanalyzedCalls;
    
    public CallGraphBuilder(ClassFileLoader classFileLoader) {
        this(classFileLoader, new EmptyNodeSet());
    }
    
    public CallGraphBuilder(ClassFileLoader classFileLoader, NodeSet ignoreSet) {
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
    
    public void addRoot(Root root) throws Exception {
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
        return !callGraph.hasClass(className) && !shouldIgnoreClass(className);
    }
    
    private boolean shouldIgnoreClass(String className) {
        return className.startsWith("[") || ignoreSet.containsClass(className);
    }
    
    private boolean shouldIgnoreMethod(MethodPath path) {
        return shouldIgnoreClass(path.getOwner()) || ignoreSet.containsMethod(path);
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
        if (!isBasicArrayClass(internalName) && !shouldIgnoreClass(internalName)) {
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
        ClassNode cls = callGraph.getClass(methodPath.getOwner());
        return cls.getMethod(methodPath.getName(), new MethodType(methodPath.getDesc()));
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
            if (shouldIgnoreMethod(callee)) {
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
            if ((version & Opcodes.ACC_INTERFACE) == 0) { // Interfaces have Object as their superclass. We don't need that info in our callgraph.
                ClassNode superNode = getDependency(superName);
                cls = callGraph.addClass(name, superNode);
            }
            for (String interfaceName : interfaces) {
                cls.addInterface(getDependency(interfaceName));
            }
        }

        private ClassNode getDependency(String superName) {
            ClassNode depNode = null;
            if (superName != null) {
                if (callGraph.hasClass(superName)) {
                    depNode = callGraph.getClass(superName);
                } else {
                    try {
                        depNode = discoverClass(superName);
                    } catch (ClassNotFoundException e) {
                        throw new CheckedExceptionWrapper(e);
                    } catch (IOException e) {
                        throw new CheckedExceptionWrapper(e);
                    }
                }
            }
            return depNode;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodPath path = new MethodPath(cls.getName(), name, desc);
            if (shouldIgnoreMethod(path)) {
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
            if (!foundFpMath && hasFloatResult(opcode)) {
                result.localFpMathMethods().add(methodNode);
                foundFpMath = true;
            }
        }
        
        private boolean hasFloatResult(int opcode) {
            
            /*
             * Only operations that produce floating point results
             * can be non-strictfp. Operations that take floats and produce
             * something else are innocuous.
             * 
             * It's also safe to load and save floats as long as no arithmetic is
             * performed on them. A float converted to the extended value set can always
             * be converted back to its original value:
             * 
             *   "Note that the constraints in Table 4.1 are designed so that every element of the
             *    ﬂoat value set is necessarily also an element of the ﬂoat-extended-exponent value
             *    set, the double value set, and the double-extended-exponent value set. Likewise,
             *    each element of the double value set is necessarily also an element of the doubleextended-exponent value set. Each extended-exponent value set has a larger range
             *    of exponent values than the corresponding standard value set, but does not have
             *    more precision"
             *   -- [http://java.sun.com/docs/books/jls/strictfp-changes.pdf]
             * 
             */
            
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
            case F2D:
            case D2F:
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
