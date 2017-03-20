package com.soundcloud.android.di;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.tests.NetworkMappings;

import android.content.res.Resources;

public class TestApiModule extends ApiModule {

    protected String providePublicApiBaseUrl(Resources resources) {
        return resources.getString(R.string.public_api_base_url);
    }

    protected String provideMobileApiBaseUrl(Resources resources) {
        return NetworkMappings.MOCK_API_ADDRESS;
    }
}
