package com.soundcloud.android.policies;

import com.soundcloud.android.utils.NonFatalRuntimeException;

public class PolicyUpdateFailure extends NonFatalRuntimeException {
    public PolicyUpdateFailure(Throwable cause) {
        super("Failed updating policies", cause);
    }
}
