package com.soundcloud.android.analytics.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
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
}
