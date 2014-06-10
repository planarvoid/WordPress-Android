package com.soundcloud.android.tests;

public class ViewNotFoundException extends ViewException {
    public ViewNotFoundException() {
        super();
    }

    public ViewNotFoundException(String detailMessage) {
        super("View not found: "+detailMessage);
    }

    public ViewNotFoundException(Exception e) {
        super(e);
    }
}
