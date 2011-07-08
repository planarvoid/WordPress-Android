package com.soundcloud.android.service.beta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.soundcloud.android.service.beta.BetaService.TAG;

/** @noinspection UnusedDeclaration*/
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        Log.d(TAG, "onReceive("+ctx+","+intent+")");

        if (BetaService.shouldCheckForUpdates(ctx)) {
            BetaService.scheduleCheck(ctx, false);
        }
    }
}
