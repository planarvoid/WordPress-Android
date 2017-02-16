package com.soundcloud.android.offline;

import com.soundcloud.android.properties.ApplicationProperties;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class OfflineModule {

    static final String STRICT_SSL_CLIENT = "StrictSSLHttpClient";

    @Provides
    @Singleton
    @Named(STRICT_SSL_CLIENT)
    public OkHttpClient provideOkHttpClient(ApplicationProperties applicationProperties,
                                            OkHttpClient okHttpClient) {
        if (applicationProperties.isDevelopmentMode()) {
            return okHttpClient;
        } else {
            return okHttpClient.newBuilder().hostnameVerifier(new SoundCloudHostnameVerifier()).build();
        }
    }
}
