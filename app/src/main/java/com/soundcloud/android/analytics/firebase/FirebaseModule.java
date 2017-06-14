package com.soundcloud.android.analytics.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.soundcloud.android.properties.ApplicationProperties;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class FirebaseModule {

    @Provides
    @Singleton
    FirebaseOptions provideFirebaseOptions() {
        return FirebaseApp.getInstance().getOptions();
    }

    @Provides
    @Singleton
    FirebaseRemoteConfig provideFirebaseRemoteConfig(ApplicationProperties applicationProperties) {
        final FirebaseRemoteConfigSettings remoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(applicationProperties.isDevelopmentMode())
                .build();

        final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setConfigSettings(remoteConfigSettings);

        return remoteConfig;
    }
}
