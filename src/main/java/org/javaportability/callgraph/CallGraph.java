package org.javaportability.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.javaportability.misc.MethodPath;
import org.objectweb.asm.MethodType;
import org.objectweb.asm.Type;

public class CallGraph {
    
    public class ClassNode {
        private String name;          // The "internal name" of the class
        private ClassNode superclass; // Possibly null
        private List<ClassNode> interfaces;
        private List<MethodNode> methods;
        
        private ClassNode(String name, ClassNode superclass) {
            this.name = name;
            this.superclass = superclass;
            this.interfaces = new LinkedList<ClassNode>();
            this.methods = new ArrayList<MethodNode>();
        }
        
        public String getName() {
            return name;
        }
        
        public ClassNode getSuperclass() {
            return superclass;
        }
        
        public void addInterface(ClassNode interfaceNode) {
            this.interfaces.add(interfaceNode);
        }
        
        public List<ClassNode> getLocalInterfaces() {
            return Collections.unmodifiableList(interfaces);
        }
        
        public List<ClassNode> getHierarchy() {
            List<ClassNode> result = new LinkedList<ClassNode>();
            ClassNode cls = this;
            while (cls != null) {
                result.add(cls);
                for (ClassNode iface : cls.getLocalInterfaces()) {
                    result.addAll(iface.getHierarchy());
                }
                cls = cls.superclass;
            }
            return result;
        }
        
        public MethodNode addMethod(String name, MethodType type) {
            if (hasLocalMethod(name, type)) {
                throw new IllegalArgumentException("Method already added to class: " + name + " " + type.getDescriptor());
            }
            MethodNode method = new MethodNode(name, type, this);
            methods.add(method);
            return method;
        }
        
        public MethodNode tryGetMethod(String name, MethodType type) {
            for (ClassNode cls : getHierarchy()) {
                MethodNode m = cls.tryGetLocalMethod(name, type);
                if (m != null) {
                    return m;
                }
            }
            return null;
        }
        
        public MethodNode getMethod(String name, MethodType type) {
            MethodNode m = tryGetMethod(name, type);
            if (m == null) {
                throw new IllegalArgumentException("No such method " + this.name + " :: " + name + " " + type);
            }
            return m;
        }
        
        public boolean hasMethod(String name, MethodType type) {
            return tryGetMethod(name, type) != null;
        }
        
        public MethodNode tryGetLocalMethod(String name, MethodType type) {
            for (MethodNode m : methods) {
                if (m.getName().equals(name) && m.getType().equals(type)) {
                    return m;
                }
            }
            return null;
        }
        
        public MethodNode getLocalMethod(String name, MethodType type) {
            MethodNode m = tryGetLocalMethod(name, type);
            if (m == null) {
                throw new IllegalArgumentException("No such local method " + this.name + " :: " + name + " " + type);
            }
            return m;
        }
        
        public boolean hasLocalMethod(String name, MethodType type) {
            return tryGetLocalMethod(name, type) != null;
        }
        
        public List<MethodNode> getLocalMethods() {
            return Collections.unmodifiableList(methods);
        }
        
        public List<MethodNode> getMethodsIncludingInherited() {
            List<MethodNode> result = new ArrayList<MethodNode>();
            for (ClassNode cls : getHierarchy()) {
                result.addAll(cls.methods);
            }
            return result;
        }
        
        @Override
        public String toString() {
            return "Class " + name;
        }
    }
    
    public class MethodNode {
        private String name;
        private MethodType type;
        private ClassNode owner;
        private List<CallSite> outgoingCalls;
        private List<CallSite> incomingCalls;
        
        private MethodNode(String name, MethodType type, ClassNode owner) {
            this.name = name;
            this.type = type;
            this.owner = owner;
            this.outgoingCalls = new ArrayList<CallSite>();
            this.incomingCalls = new ArrayList<CallSite>();
        }
        
        public String getName() {
            return name;
        }
        
        public MethodType getType() {
            return type;
        }
        
        public ClassNode getOwner() {
            return owner;
        }
        
        public String getDesc() {
            return type.getDescriptor();
        }
        
        public MethodPath getPath() {
            return new MethodPath(owner.getName(), name, getDesc());
        }
        
        public List<CallSite> getOutgoingCalls() {
            return Collections.unmodifiableList(outgoingCalls);
        }
        
        public List<CallSite> getIncomingCalls() {
            return Collections.unmodifiableList(incomingCalls);
        }
        
        @Override
        public String toString() {
            return "method " + owner.getName() + " :: " + name + " " + getDesc();
        }
    }
    
    public class CallSite {
        private MethodNode from;
        private MethodNode to;
        
        private CallSite(MethodNode from, MethodNode to) {
            this.from = from;
            this.to = to;
        }
        
        public MethodNode getFrom() {
            return from;
        }
        
        public MethodNode getTo() {
            return to;
        }
        
        @Override
        public String toString() {
            return "call from " + from.getName() + " to " + to.getName();
        }
    }
    
    private HashMap<String, ClassNode> classes;
    
    public CallGraph() {
        this.classes = new HashMap<String, CallGraph.ClassNode>();
    }
    
    public ClassNode addClass(String internalName) {
        return addClass(internalName, null);
    }
    
    public ClassNode addClass(String internalName, ClassNode superclass) {
        if (hasClass(internalName)) {
            throw new IllegalArgumentException("Class already added: " + internalName);
        }
        ClassNode cls = new ClassNode(internalName, superclass);
        classes.put(internalName, cls);
        return cls;
    }
    
    public boolean hasClass(String internalName) {
        return classes.containsKey(internalName);
    }
    
    public boolean hasClass(Class<?> cls) {
        return hasClass(Type.getInternalName(cls));
    }
    
    public ClassNode getClass(String internalName) {
        ClassNode cn = tryGetClass(internalName);
        if (cn == null) {
            throw new IllegalStateException("No such class in the call graph: " + internalName);
        }
        return cn;
    }
    
    public ClassNode getClass(Class<?> cls) {
        return getClass(Type.getInternalName(cls));
    }
    
    public Collection<ClassNode> getClasses() {
        return classes.values();
    }
    
    public ClassNode tryGetClass(String internalName) {
        return classes.get(internalName);
    }
    
    public CallSite addCall(MethodNode from, MethodNode to) {
        CallSite cs = new CallSite(from, to);
        from.outgoingCalls.add(cs);
        to.incomingCalls.add(cs);
        return cs;
    }
}
