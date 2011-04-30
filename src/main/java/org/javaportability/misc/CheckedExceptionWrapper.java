package org.javaportability.misc;

public class CheckedExceptionWrapper extends RuntimeException {
    public CheckedExceptionWrapper(Throwable cause) {
        super(cause);
    }
}
