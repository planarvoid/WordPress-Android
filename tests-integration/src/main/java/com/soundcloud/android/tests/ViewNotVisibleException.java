package com.soundcloud.android.tests;

public class ViewNotVisibleException extends ViewException {
    public ViewNotVisibleException() {
        super("View is not visible, cannot click it!");
    }
}
