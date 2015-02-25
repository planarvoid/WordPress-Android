package com.soundcloud.android.api;


import android.content.res.Resources;

import com.soundcloud.android.R;

import javax.inject.Inject;

public class HttpProperties {

    private final String mobileApiBaseUrl;
    private final String publicApiBaseUrl;

    @Inject
    public HttpProperties(Resources resources) {
        this.mobileApiBaseUrl = resources.getString(R.string.mobile_api_base_url);
        this.publicApiBaseUrl = resources.getString(R.string.public_api_base_url);
    }

    public String getMobileApiBaseUrl() {
        return mobileApiBaseUrl;
    }

    public String getPublicApiBaseUrl() {
        return publicApiBaseUrl;
    }

    /**
     * @return e.g. http://api-mobile.soundcloud.com instead of https://
     */
    public String getMobileApiHttpUrl() {
        return mobileApiBaseUrl.replaceFirst("https://", "http://");
    }
}
