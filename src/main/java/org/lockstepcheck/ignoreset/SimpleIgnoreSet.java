package org.lockstepcheck.ignoreset;

import java.util.HashSet;

import org.lockstepcheck.misc.MethodPath;
import org.objectweb.asm.Type;

public class SimpleIgnoreSet implements IgnoreSet {
    
    private HashSet<String> classNames;
    private HashSet<MethodPath> methods;
    
    public SimpleIgnoreSet() {
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
