package com.soundcloud.android.tests.crypto;

import android.content.Context;
import android.content.SharedPreferences;

final class DeviceKey {

    private Context context;

    public DeviceKey(Context context) {
        this.context = context;
    }

    public boolean isValid() {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("device_keys", Context.MODE_PRIVATE);
        String encodedKey = sharedPreferences.getString("device_key", "");
        String encodedIV = sharedPreferences.getString("device_key.iv", "");
        return encodedKey.length() > 0 && encodedIV.length() > 0;
    }


}
