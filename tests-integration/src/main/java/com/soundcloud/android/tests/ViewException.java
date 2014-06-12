package com.soundcloud.android.tests;

public class ViewException extends RuntimeException {
    public ViewException() {
        super();
    }

    public ViewException(String detailMessage) {
        super(detailMessage);
    }

    public ViewException(Exception e) {
        super(e);
    }
}
