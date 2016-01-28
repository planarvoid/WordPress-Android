package com.soundcloud.android.policies;

class PolicyUpdateFailure extends RuntimeException {
    public PolicyUpdateFailure(Throwable cause) {
        super("Failed updating policies", cause);
    }
}
