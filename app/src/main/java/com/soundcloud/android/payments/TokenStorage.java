package com.soundcloud.android.payments;

import org.jetbrains.annotations.Nullable;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

class TokenStorage {

    private static final String KEY_PENDING_URN = "pending_transaction_urn";

    private final SharedPreferences sharedPreferences;

    @Inject
    public TokenStorage(@Named("Payments") SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void setCheckoutToken(String token) {
        sharedPreferences.edit().putString(KEY_PENDING_URN, token).apply();
    }

    @Nullable
    public String getCheckoutToken() {
        return sharedPreferences.getString(KEY_PENDING_URN, null);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}
