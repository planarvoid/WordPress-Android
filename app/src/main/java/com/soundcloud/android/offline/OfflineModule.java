package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.properties.ApplicationProperties;
import com.squareup.okhttp.OkHttpClient;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class,
        injects = {
                OfflineContentService.class,
                OfflineSettingsStorage.class,
                OfflineLikesDialog.class,
                OfflineContentStartReceiver.class
        })
public class OfflineModule {

    public static final String STRICT_SSL_CLIENT = "StrictSSLHttpClient";

    @Provides
    @Named(STRICT_SSL_CLIENT)
    public OkHttpClient provideOkHttpClient(ApplicationProperties applicationProperties, OkHttpClient defaultClient) {
        final OkHttpClient client = defaultClient.clone();
        if (!applicationProperties.isDebugBuild()) {
            client.setHostnameVerifier(new SoundCloudHostnameVerifier());
        }
        return client;
    }
}
