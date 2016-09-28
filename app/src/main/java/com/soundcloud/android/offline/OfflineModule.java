package com.soundcloud.android.offline;

import static com.soundcloud.android.api.ApiModule.API_HTTP_CLIENT;

import com.soundcloud.android.properties.ApplicationProperties;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class OfflineModule {

    public static final String STRICT_SSL_CLIENT = "StrictSSLHttpClient";

    @Provides
    @Singleton
    @Named(STRICT_SSL_CLIENT)
    public OkHttpClient provideOkHttpClient(ApplicationProperties applicationProperties,
                                            @Named(API_HTTP_CLIENT) OkHttpClient defaultClient) {
        final OkHttpClient client = defaultClient.clone();
        if (!applicationProperties.isDevelopmentMode()) {
            client.setHostnameVerifier(new SoundCloudHostnameVerifier());
        }
        return client;
    }
}
