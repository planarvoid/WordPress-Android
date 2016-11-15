package com.soundcloud.android.testsupport;

import com.google.firebase.FirebaseApp;

import android.app.Application;

/**
 * Used in Robolectric tests so that we don't go through our untestable Application#onCreate
 */
public class ApplicationStub extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initializeFirebase();
    }

    private void initializeFirebase() {
        if (FirebaseApp.getApps(this).size() == 0) {
            FirebaseApp.initializeApp(this);
        }
    }
}
