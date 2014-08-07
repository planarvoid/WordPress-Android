package com.soundcloud.android.rx;

import com.soundcloud.android.utils.ErrorUtils;
import rx.plugins.RxJavaErrorHandler;

/**
 * See https://github.com/Netflix/RxJava/issues/969
 */
public class RxGlobalErrorHandler extends RxJavaErrorHandler {
    @Override
    public void handleError(Throwable t) {
        ErrorUtils.handleThrowable(t, getClass());
    }
}
