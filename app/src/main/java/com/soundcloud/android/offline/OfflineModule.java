package com.soundcloud.android.offline;

import com.soundcloud.android.properties.ApplicationProperties;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

import javax.inject.Named;
import javax.inject.Singleton;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class OfflineModule {

    static final String STRICT_SSL_CLIENT = "StrictSSLHttpClient";

    @Provides
    @Singleton
    @Named(STRICT_SSL_CLIENT)
    static OkHttpClient provideOkHttpClient(ApplicationProperties applicationProperties,
                                     OkHttpClient okHttpClient) {
        if (applicationProperties.isDevelopmentMode()) {
            return okHttpClient;
        } else {
            return okHttpClient.newBuilder().hostnameVerifier(new SoundCloudHostnameVerifier()).build();
        }
    }

    @Provides
    static IOfflinePropertiesProvider provideOfflinePropertiesProvider(OfflinePropertiesProvider offlinePropertiesProvider) {
        return offlinePropertiesProvider;
    }
}
