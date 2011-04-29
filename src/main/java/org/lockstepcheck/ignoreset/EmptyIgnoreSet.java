package org.lockstepcheck.ignoreset;

import org.lockstepcheck.misc.MethodPath;

public class EmptyIgnoreSet implements IgnoreSet {
    
    public EmptyIgnoreSet() {
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
