package com.soundcloud.android.tests.viewelements;

import com.soundcloud.android.tests.ViewException;

public class ViewNotVisibleException extends ViewException {
    public ViewNotVisibleException() {
        super("View is not visible, cannot click it!");
    }
}
