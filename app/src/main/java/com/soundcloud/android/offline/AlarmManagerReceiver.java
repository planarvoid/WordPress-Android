package com.soundcloud.android.offline;

import com.soundcloud.android.policies.PolicyUpdateService;
import com.soundcloud.android.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmManagerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(OfflineContentService.ACTION_START.equals(intent.getAction())) {
            Log.d(OfflineContentService.TAG, "Offline Content Start Receiver notified. Starting service.");
            OfflineContentService.start(context);

        } else if (PolicyUpdateService.ACTION_START.equals(intent.getAction())) {
            Log.d(PolicyUpdateService.TAG, "Policy Update Start Receiver notified. Starting service.");
            PolicyUpdateService.start(context);
        }
    }
}
