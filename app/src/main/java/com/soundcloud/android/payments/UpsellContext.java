package com.soundcloud.android.payments;

import android.content.Intent;

public enum UpsellContext {

    DEFAULT, ADS, OFFLINE, PREMIUM_CONTENT;

    private static final String intentExtra = "upsell_context";

    public void addTo(Intent intent) {
        intent.putExtra(intentExtra, ordinal());
    }

    public static UpsellContext from(Intent intent) {
        return values()[intent.getIntExtra(intentExtra, DEFAULT.ordinal())];
    }

}
