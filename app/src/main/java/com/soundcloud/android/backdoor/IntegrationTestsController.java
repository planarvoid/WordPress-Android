package com.soundcloud.android.backdoor;

import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;

import javax.inject.Inject;

public class IntegrationTestsController {
    private final Context context;
    private final IntegrationTestsBroadcastReceiver receiver;

    @Inject
    public IntegrationTestsController(Context context, IntegrationTestsBroadcastReceiver receiver) {
        this.context = context;
        this.receiver = receiver;
    }

    public void subscribe() {
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, receiver.getIntentFilter());
    }
}
