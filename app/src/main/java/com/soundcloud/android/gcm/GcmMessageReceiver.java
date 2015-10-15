package com.soundcloud.android.gcm;

import com.soundcloud.android.SoundCloudApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class GcmMessageReceiver extends BroadcastReceiver {

    @Inject GcmMessageHandler gcmMessageHandler;

    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);
        gcmMessageHandler.handleMessage(intent);
    }
}
