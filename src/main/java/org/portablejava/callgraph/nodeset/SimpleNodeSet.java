package org.portablejava.callgraph.nodeset;

import java.util.HashSet;

import org.objectweb.asm.Type;
import org.portablejava.misc.MethodPath;

public class SimpleNodeSet implements NodeSet {
    
    private HashSet<String> classNames;
    private HashSet<MethodPath> methods;
    
    public SimpleNodeSet() {
        classNames = new HashSet<String>();
        methods = new HashSet<MethodPath>();
    }
    
    public void addClass(String name) {
        classNames.add(name);
    }
    
    public void addClass(Class<?> cls) {
        addClass(Type.getInternalName(cls));
    }
    
    public void addMethod(MethodPath m) {
        methods.add(m);
    }
    
    @Override
    public boolean containsClass(String className) {
        return classNames.contains(className);
    }
    
    @Override
    public boolean containsMethod(MethodPath path) {
        if (this.containsClass(path.getOwner())) {
            return true;
        } else {
            return methods.contains(path);
        }
    }
}
