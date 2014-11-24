package com.soundcloud.android.framework.viewelements;

public class ViewNotVisibleException extends ViewException {
    public ViewNotVisibleException() {
        super("View is not visible, cannot click it!");
    }
}
