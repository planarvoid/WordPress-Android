package com.soundcloud.android.service.beta;

import static com.soundcloud.android.service.beta.BetaService.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class WifiMonitor extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
            if (info.isConnected()) {
                boolean requireWifi = PreferenceManager
                               .getDefaultSharedPreferences(context)
                               .getBoolean(BetaService.PREF_REQUIRE_WIFI, true);

                if (requireWifi && BetaService.isPendingBeta(context)) {
                    Log.d(TAG, "WifiMonitor: scheduling betaservice check");
                    BetaService.scheduleNow(context, 0);
                }
            }
        }
    }
}
