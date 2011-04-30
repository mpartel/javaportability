package org.portablejava.analysis.results;

import java.util.Iterator;

import org.portablejava.callgraph.CallGraph.MethodNode;
import org.portablejava.misc.Misc;

public class CallPath implements Iterable<MethodNode> {
    private final MethodNode method;
    private final CallPath next;
    
    public CallPath(MethodNode method) {
        this(method, null);
    }
    
    public CallPath(MethodNode method, CallPath next) {
        this.method = method;
        this.next = next;
    }
    
    public static Object make(MethodNode... ms) {
        CallPath cp = null;
        for (int i = ms.length - 1; i >= 0; i--) {
            cp = new CallPath(ms[i], cp);
        }
        return cp;
    }
    
    public MethodNode getMethod() {
        return method;
    }
    
    public CallPath getNext() {
        return next;
    }
    
    public boolean hasNext() {
        return next != null;
    }
    
    @Override
    public Iterator<MethodNode> iterator() {
        return new CallPathIterator(this);
    }
    
    @Override
    public int hashCode() {
        return method.hashCode() << (next == null ? 1 : 2);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof CallPath) {
            CallPath other = ((CallPath)obj);
            return this.method.equals(other.method) &&
                   ((this.next == null && other.next == null) ||
                    (this.next != null && this.next.equals(other.next)));
        } else {
            return false;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodToString(this.method));
        CallPath p = this.next;
        while (p != null) {
            sb.append(" -> ");
            sb.append(methodToString(p.method));
            p = p.next;
        }
        return sb.toString();
    }
    
    private static String methodToString(MethodNode m) {
        return Misc.shortClassName(m.getOwner()) + "::" + m.getName();
    }

    private static class CallPathIterator implements Iterator<MethodNode> {
        private CallPath p;
        
        public CallPathIterator(CallPath p) {
            this.p = p;
        }
        
        @Override
        public boolean hasNext() {
            return p != null;
        }
        
        @Override
        public MethodNode next() {
            CallPath q = p;
            p = p.next;
            return q.method;
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
