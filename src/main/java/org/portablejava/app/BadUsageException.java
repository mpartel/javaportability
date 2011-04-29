package org.portablejava.app;

public class BadUsageException extends RuntimeException {
    public BadUsageException(String msg) {
        super(msg);
    }
}
