package com.soundcloud.android.api;

public class ApiMapperException extends Exception {

    public ApiMapperException(Throwable cause) {
        super("Failed mapping body of request/response", cause);
    }

    public ApiMapperException(String message) {
        super(message);
    }
}
