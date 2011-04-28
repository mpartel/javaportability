package org.lockstepcheck.misc;

public class CheckedExceptionWrapper extends RuntimeException {
    public CheckedExceptionWrapper(Throwable cause) {
        super(cause);
    }
}
