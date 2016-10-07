package com.soundcloud.android.utils;

public class NonFatalRuntimeException extends RuntimeException {

    public NonFatalRuntimeException() {}

    public NonFatalRuntimeException(String message) {
        super(message);
    }

    public NonFatalRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
