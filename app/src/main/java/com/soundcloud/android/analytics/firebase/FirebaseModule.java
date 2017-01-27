package com.soundcloud.android.analytics.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class FirebaseModule {

    static final String FIREBASE_HTTP_CLIENT = "FirebaseHttpClient";

    @Provides
    @Singleton
    FirebaseOptions provideFirebaseOptions() {
        return FirebaseApp.getInstance().getOptions();
    }

    @Provides
    @Singleton
    @Named(FIREBASE_HTTP_CLIENT)
    OkHttpClient provideOkHttpClient(OkHttpClient.Builder clientBuilder) {
        return clientBuilder.build();
    }
}
