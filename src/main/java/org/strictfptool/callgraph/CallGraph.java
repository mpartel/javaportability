package org.strictfptool.callgraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.MethodType;

public class CallGraph {
    public interface ClassAnnotation {
    }
    
    public interface MethodAnnotation {
    }
    
    public class ClassNode extends AbstractHavingAnnotations<ClassAnnotation> {
        private String name; // The "internal name" of the class
        private List<MethodNode> methods;
        
        private ClassNode(String name) {
            this.name = name;
            this.methods = new ArrayList<CallGraph.MethodNode>();
        }
        
        public String getName() {
            return name;
        }
        
        public MethodNode addMethod(String name, MethodType type) {
            MethodNode method = new MethodNode(name, type, this);
            methods.add(method);
            return method;
        }
        
        @Override
        public String toString() {
            return "Class " + name;
        }
    }
    
    public class MethodNode extends AbstractHavingAnnotations<MethodAnnotation> {
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
        
        public List<CallSite> getOutgoingCalls() {
            return Collections.unmodifiableList(outgoingCalls);
        }
        
        public List<CallSite> getIncomingCalls() {
            return Collections.unmodifiableList(incomingCalls);
        }
        
        @Override
        public String toString() {
            return "Method " + owner.getName() + " :: " + name + " [" + type.getDescriptor() + "]";
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
            return "Call from " + from.getName() + " to " + to.getName();
        }
    }
    
    private HashMap<String, ClassNode> classes;
    
    public CallGraph() {
        this.classes = new HashMap<String, CallGraph.ClassNode>();
    }
    
    public ClassNode addClass(String internalName) {
        ClassNode cls = new ClassNode(internalName);
        classes.put(internalName, cls);
        return cls;
    }
    
    public CallSite addCall(MethodNode from, MethodNode to) {
        CallSite cs = new CallSite(from, to);
        from.outgoingCalls.add(cs);
        to.incomingCalls.add(cs);
        return cs;
    }
    
    
}
