package com.soundcloud.android;

import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public final class CodeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Consts.SECRET_CODE_ACTION.equals(intent.getAction())) {
            if (Consts.SecretCodes.TOGGLE_ERROR_REPORTING.equalsIgnoreCase(intent.getData().getHost())) {
                SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(context);
                final boolean enabled = !pm.getBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, false);
                SharedPreferencesUtils.apply(pm.edit().putBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, enabled));

                if (Log.isLoggable(CloudPlaybackService.TAG, Log.DEBUG)) {
                    Log.d(CloudPlaybackService.TAG, "toggling error reporting (enabled=" + enabled + ")");
                }

                Toast.makeText(context, context.getString(R.string.playback_error_logging,
                        context.getText(enabled ? R.string.enabled : R.string.disabled)), Toast.LENGTH_LONG).show();
            }
        }
    }
}
