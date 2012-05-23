package com.soundcloud.android.service.playback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public final class ToggleErrorReportingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (Consts.SECRET_CODE_ACTION.equals(action)) {
            SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean newValue = !pm.getBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, false);
            SharedPreferencesUtils.apply(pm.edit().putBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, newValue));

            if (Log.isLoggable(CloudPlaybackService.TAG, Log.DEBUG)) {
                Log.d(CloudPlaybackService.TAG, "toggling error reporting (enabled=" + newValue + ")");
            }

            Toast.makeText(context, context.getString(R.string.playback_error_logging,
                    newValue ? context.getText(R.string.enabled) : context.getText(R.string.disabled)), Toast.LENGTH_LONG).show();
        }
    }
}
