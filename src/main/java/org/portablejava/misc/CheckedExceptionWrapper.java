package org.portablejava.misc;

public class CheckedExceptionWrapper extends RuntimeException {
    public CheckedExceptionWrapper(Throwable cause) {
        super(cause);
    }
}
