package com.soundcloud.android.gcm;

import com.google.firebase.iid.FirebaseInstanceId;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class InstanceIdWrapper {

    @Inject
    public InstanceIdWrapper() {
    }

    @Nullable
    public String getToken() {
        return FirebaseInstanceId.getInstance().getToken();
    }

}
