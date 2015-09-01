package com.soundcloud.android.gcm;

import com.google.android.gms.iid.InstanceID;

import android.content.Context;

import javax.inject.Inject;
import java.io.IOException;

public class InstanceIdWrapper {

    @Inject
    public InstanceIdWrapper() {
    }

    public String getToken(Context context, String defaultSenderId, String scope) throws IOException {
        return InstanceID.getInstance(context).getToken(defaultSenderId, scope, null);
    }

}
