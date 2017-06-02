package com.soundcloud.android.deeplinks;

/**
 * Exception to indicate that Uri resolving finished unexpected.
 */
public class UriResolveException extends Exception {
    public UriResolveException() {
    }

    public UriResolveException(String message) {
        super(message);
    }

    public UriResolveException(String message, Throwable cause) {
        super(message, cause);
    }
}
