package com.soundcloud.android.playback;

import com.soundcloud.android.utils.ScTextUtils;

import android.support.annotation.Nullable;

import java.util.Locale;

public class CacheConfig {

    static final long MIN_SIZE_BYTES = 60  * 1024 * 1024; // 60MB
    static final long MAX_SIZE_BYTES = 500 * 1024 * 1024; // 500MB

    public static long getCacheSize(@Nullable String countryCode){
        if (ScTextUtils.isBlank(countryCode)
                || Locale.US.getCountry().equalsIgnoreCase(countryCode)
                || Locale.GERMANY.getCountry().equalsIgnoreCase(countryCode)
                || Locale.FRANCE.getCountry().equalsIgnoreCase(countryCode)
                || Locale.UK.getCountry().equalsIgnoreCase(countryCode)) {
            return MIN_SIZE_BYTES;

        } else {
            return MAX_SIZE_BYTES;
        }

    }

}
