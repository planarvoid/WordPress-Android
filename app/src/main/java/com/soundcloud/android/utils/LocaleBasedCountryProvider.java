package com.soundcloud.android.utils;

import javax.inject.Inject;
import java.util.Locale;

public class LocaleBasedCountryProvider implements CountryProvider {

    @Inject
    public LocaleBasedCountryProvider() {
        // for injection
    }

    @Override
    public String getCountryCode() {
        return Locale.getDefault().getCountry();
    }

}
