package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.properties.ApplicationProperties;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module(addsTo = ApplicationModule.class,
        injects = {
                OfflineContentService.class,
                OfflineSettingsStorage.class,
                OfflineLikesDialog.class,
                OfflineContentStartReceiver.class
        })
public class OfflineModule {

    @Provides
    @Named("StrictSSLHttpClient")
    public OkHttpClient provideOkHttpClient(ApplicationProperties applicationProperties) {
        final OkHttpClient client = new OkHttpClient();
        if (!applicationProperties.isDebugBuild()) {
            client.setHostnameVerifier(new SoundCloudHostnameVerifier());
        }
        return client;
    }
}
