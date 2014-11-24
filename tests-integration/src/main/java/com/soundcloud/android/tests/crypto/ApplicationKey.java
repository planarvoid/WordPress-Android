package com.soundcloud.android.tests.crypto;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

final class ApplicationKey {

    private Activity activity;

    public ApplicationKey(Activity activity) {
        this.activity = activity;
    }

    public boolean isValid() {
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("device_keys", Context.MODE_PRIVATE);
        String encodedKey = sharedPreferences.getString("device_key", "");
        String encodedIV = sharedPreferences.getString("device_key.iv", "");
        return encodedKey.length() > 0 && encodedIV.length() > 0;
    }


}
