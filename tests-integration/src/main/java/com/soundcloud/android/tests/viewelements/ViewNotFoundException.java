package com.soundcloud.android.tests.viewelements;

import com.soundcloud.android.tests.ViewException;

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
