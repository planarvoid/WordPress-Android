package com.soundcloud.android.di;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.tests.NetworkMappings;
import dagger.Provides;

import android.content.res.Resources;

import javax.inject.Named;
import javax.inject.Singleton;

public class TestApiModule extends ApiModule {

    @Provides
    @Singleton
    @Named(PUBLIC_API_BASE_URL)
    protected String providePublicApiBaseUrl(Resources resources) {
        return resources.getString(R.string.public_api_base_url);
    }

    @Provides
    @Singleton
    @Named(MOBILE_API_BASE_URL)
    protected String provideMobileApiBaseUrl(Resources resources) {
        return NetworkMappings.MOCK_API_ADDRESS;
    }
}
