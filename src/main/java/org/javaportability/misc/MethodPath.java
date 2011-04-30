package org.javaportability.misc;

import org.objectweb.asm.Type;

public final class MethodPath implements Comparable<MethodPath> {
    private final String cls;
    private final String name;
    private final String desc;
    
    public MethodPath(String cls, String name, String desc) {
        this.cls = cls;
        this.name = name;
        this.desc = desc;
    }
    
    public MethodPath(Class<?> cls, String name, String desc) {
        this(Type.getInternalName(cls), name, desc);
    }
    
    public String getOwner() {
        return cls;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDesc() {
        return desc;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MethodPath) {
            MethodPath that = (MethodPath)obj;
            return this.cls.equals(that.cls) &&
                   this.name.equals(that.name) &&
                   this.desc.equals(that.desc);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return cls + " :: " + name + " " + desc;
    }

    @Override
    public int compareTo(MethodPath that) {
        return this.toString().compareTo(that.toString());
    }
}
