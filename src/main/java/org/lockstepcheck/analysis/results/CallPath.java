package org.lockstepcheck.analysis.results;

import java.util.Iterator;

import org.lockstepcheck.callgraph.CallGraph.MethodNode;

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
    public String toString() {
        return this.toString("\n");
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
    
    public String toString(String separator) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.method.toString());
        CallPath p = this.next;
        while (p != null) {
            sb.append(separator);
            sb.append(p.method.toString());
            p = p.next;
        }
        return sb.toString();
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
