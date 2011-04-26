package org.strictfptool.callgraph;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.strictfptool.ClassFileLoader;
import org.strictfptool.MethodPath;
import org.strictfptool.annotations.DoesLocalFpMath;
import org.strictfptool.annotations.NativeMethod;
import org.strictfptool.annotations.StrictfpMethod;
import org.strictfptool.annotations.TransitivelyAnalyzed;
import org.strictfptool.callgraph.CallGraph.ClassNode;
import org.strictfptool.callgraph.CallGraph.MethodNode;
import org.strictfptool.ignoreset.EmptyIgnoreSet;
import org.strictfptool.ignoreset.IgnoreSet;
import org.strictfptool.misc.CheckedExceptionWrapper;

public class CallGraphBuilder extends EmptyVisitor {

    private CallGraph callGraph;
    private ClassFileLoader classFileLoader;
    private IgnoreSet ignoreSet;
    private Queue<String> classQueue;
    private Queue<MethodPath> methodQueue;
    private HashMap<MethodNode, List<MethodPath>> unanalyzedCalls;
    
    public static CallGraph buildCallGraph(ClassFileLoader loader, Set<MethodPath> roots) throws IOException {
        return buildCallGraph(loader, roots, EmptyIgnoreSet.getInstance());
    }
    
    public static CallGraph buildCallGraph(ClassFileLoader loader, Set<MethodPath> roots, IgnoreSet ignoreSet) throws IOException {
        CallGraph cg = new CallGraph();
        CallGraphBuilder builder = new CallGraphBuilder(cg, loader, roots, ignoreSet);
        builder.mainLoop();
        return cg;
    }
    
    private CallGraphBuilder(CallGraph callGraph, ClassFileLoader classFileLoader, Set<MethodPath> roots, IgnoreSet ignoreSet) {
        this.callGraph = callGraph;
        this.classFileLoader = classFileLoader;
        this.ignoreSet = ignoreSet;
        this.classQueue = new LinkedList<String>();
        this.methodQueue = new LinkedList<MethodPath>(roots);
        this.unanalyzedCalls = new HashMap<MethodNode, List<MethodPath>>();
    }
    
    private void mainLoop() throws IOException {
        try {
            while (!classQueue.isEmpty() || !methodQueue.isEmpty()) {
                if (!classQueue.isEmpty()) {
                    String internalName = classQueue.remove();
                    if (!callGraph.hasClass(internalName)) {
                        discoverClass(internalName);
                    }
                } else {
                    MethodPath m = methodQueue.peek();
                    if (analyzeMethod(m)) {
                        methodQueue.remove();
                    }
                }
            }
        } catch (CheckedExceptionWrapper e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException)e.getCause();
            }
        }
    }
    
    private ClassNode discoverClass(String internalName) throws IOException {
        if (!isBasicArrayClass(internalName) && !ignoreSet.containsClass(internalName)) {
            trace("Analyzing class " + internalName);
            
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
    
    private boolean analyzeMethod(MethodPath methodPath) {
        if (!callGraph.hasClass(methodPath.getOwner())) {
            trace("Class owning method " + methodPath + " not yet discovered");
            classQueue.add(methodPath.getOwner());
            return false;
        }
        
        MethodNode methodNode = getMethodNode(methodPath);
        
        if (methodNode.hasAnnotation(TransitivelyAnalyzed.class)) {
            trace("Already analyzed method " + methodPath);
            return true; // Already analyzed
        }
        
        if (enqueueUndiscoveredCalleeClasses(methodNode) == 0) {
            methodNode.addAnnotation(TransitivelyAnalyzed.getInstance());
            
            List<MethodPath> calls = unanalyzedCalls.remove(methodNode);
            
            for (MethodPath callee : calls) {
                if (ignoreSet.containsMethod(callee)) {
                    continue;
                }
                
                MethodNode calleeNode = getMethodNode(callee);
                callGraph.addCall(methodNode, calleeNode);
                
                if (!calleeNode.hasAnnotation(TransitivelyAnalyzed.class)) {
                    methodQueue.add(callee);
                }
                
                trace("Recorded call from " + methodNode + " to " + calleeNode);
            }
            
            return true;
        } else {
            trace("Undiscovered callee classes for " + methodPath);
            return false;
        }
    }
    
    private int enqueueUndiscoveredCalleeClasses(MethodNode methodNode) {
        List<MethodPath> calls = unanalyzedCalls.get(methodNode);
        if (calls == null) {
            return 0;
        }
        
        int count = 0;
        for (MethodPath callee : calls) {
            if (!callGraph.hasClass(callee.getOwner()) && !ignoreSet.containsClass(callee.getOwner())) {
                classQueue.add(callee.getOwner());
                count += 1;
            }
        }
        
        return count;
    }

    private MethodNode getMethodNode(MethodPath methodPath) {
        return callGraph.getClass(methodPath.getOwner()).getMethod(methodPath.getName(), new MethodType(methodPath.getDesc()));
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
                    } catch (IOException e) {
                        throw new CheckedExceptionWrapper(e);
                    }
                }
            }
            return superNode;
        }
        
        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            String thisName = cls.getName();
            if (!name.equals(thisName) && outerName != null && outerName.equals(thisName)) {
                trace("Found inner class " + name);
                classQueue.add(name);
            }
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
                method.addAnnotation(StrictfpMethod.getInstance());
            }
            if (isNative(access)) {
                method.addAnnotation(NativeMethod.getInstance());
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
                methodNode.addAnnotation(DoesLocalFpMath.getInstance());
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
    
    private static final void trace(String msg) {
        //System.out.println(msg);
    }
}
