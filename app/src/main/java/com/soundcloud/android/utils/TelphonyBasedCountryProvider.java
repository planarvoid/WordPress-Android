package com.soundcloud.android.utils;

import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

import javax.inject.Inject;

public class TelphonyBasedCountryProvider implements CountryProvider {

    private TelephonyManager telephonyManager;

    @Inject
    public TelphonyBasedCountryProvider(TelephonyManager telephonyManager) {
        this.telephonyManager = telephonyManager;
    }

    @Override
    public
    @Nullable
    String getCountryCode() {
        if (telephonyManager != null) {
            final String simCountryIso = telephonyManager.getSimCountryIso();
            return simCountryIso == null ? telephonyManager.getNetworkCountryIso() : simCountryIso;
        } else {
            return null;
        }
    }
}
