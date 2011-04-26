package org.strictfptool.callgraph;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractHavingAnnotations<T> implements HavingAnnotations<T> {
    protected List<T> annotations;
    
    public AbstractHavingAnnotations() {
        this.annotations = new ArrayList<T>();
    }
    
    @SuppressWarnings("unchecked")
    public <U extends T> void addAnnotation(U annotation) {
        if (hasAnnotation((Class<? extends U>)annotation.getClass())) {
            throw new IllegalStateException("Annotation already exists: " + annotation.getClass());
        }
        this.annotations.add(annotation);
    }
    
    @Override
    public <U extends T> U getAnnotation(Class<U> type) {
        U a = tryGetAnnotation(type);
        if (a == null) {
            throw new NullPointerException("No annotation of type " + type + " on " + this);
        }
        return a;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <U extends T> U tryGetAnnotation(Class<U> type) {
        for (T annotation : annotations) {
            if (type.isInstance(annotation)) {
                return (U)annotation;
            }
        }
        return null;
    }
    
    @Override
    public <U extends T> boolean hasAnnotation(Class<U> type) {
        return tryGetAnnotation(type) != null;
    }
}
