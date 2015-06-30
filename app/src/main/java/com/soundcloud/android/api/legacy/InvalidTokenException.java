package com.soundcloud.android.api.legacy;

import java.io.IOException;

/**
 * Thrown when token is not valid.
 */
public class InvalidTokenException extends IOException {
    private static final long serialVersionUID = 1954919760451539868L;

    /**
     * @param code   the HTTP error code
     * @param status the HTTP status, or other error message
     */
    public InvalidTokenException(int code, String status) {
        super("HTTP error:" + code + " (" + status + ")");
    }
}
