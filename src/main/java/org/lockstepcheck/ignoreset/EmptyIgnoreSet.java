package org.lockstepcheck.ignoreset;

import org.lockstepcheck.misc.MethodPath;

public class EmptyIgnoreSet implements IgnoreSet {
    
    private static final EmptyIgnoreSet instance = new EmptyIgnoreSet();
    
    private EmptyIgnoreSet() {
    }
    
    public static EmptyIgnoreSet getInstance() {
        return instance;
    }
    
    @Override
    public boolean containsClass(String className) {
        return false;
    }
    
    @Override
    public boolean containsMethod(MethodPath path) {
        return false;
    }
}