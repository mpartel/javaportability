package org.strictfptool.callgraph;

public interface HavingAnnotations<T> {
    public abstract <U extends T> void addAnnotation(U annotation);
    public abstract <U extends T> boolean hasAnnotation(Class<U> type);
    public abstract <U extends T> U getAnnotation(Class<U> type);
    public abstract <U extends T> U tryGetAnnotation(Class<U> type);
}