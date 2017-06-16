package com.soundcloud.android.payments;

import com.soundcloud.android.storage.StorageModule;
import org.jetbrains.annotations.Nullable;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

class TokenStorage {

    private static final String KEY_PENDING_URN = "pending_transaction_urn";

    private final SharedPreferences sharedPreferences;

    @Inject
    TokenStorage(@Named(StorageModule.PAYMENTS) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    void setCheckoutToken(String token) {
        sharedPreferences.edit().putString(KEY_PENDING_URN, token).apply();
    }

    @Nullable
    String getCheckoutToken() {
        return sharedPreferences.getString(KEY_PENDING_URN, null);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}
