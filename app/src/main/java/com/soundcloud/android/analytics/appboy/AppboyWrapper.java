package com.soundcloud.android.analytics.appboy;


import com.appboy.Appboy;
import com.appboy.AppboyUser;
import com.appboy.models.outgoing.AppboyProperties;

import android.app.Activity;
import android.util.Base64;

import javax.inject.Inject;

class AppboyWrapper {

    private final Appboy appboy;

    @Inject
    AppboyWrapper(Appboy appboy) {
        this.appboy = appboy;
    }

    boolean openSession(Activity activity) {
        return appboy.openSession(activity);
    }

    boolean closeSession(Activity activity) {
        return appboy.closeSession(activity);
    }

    AppboyUser getCurrentUser() {
        return appboy.getCurrentUser();
    }

    AppboyUser changeUser(String userId) {
        return appboy.changeUser(encodeUserId(userId));
    }

    void requestImmediateDataFlush() {
        appboy.requestImmediateDataFlush();
    }

    boolean logCustomEvent(String eventName, AppboyProperties properties) {
        return appboy.logCustomEvent(eventName, properties);
    }

    boolean logCustomEvent(String eventName) {
        return appboy.logCustomEvent(eventName);
    }

    public String encodeUserId(String userId) {
        return Base64.encodeToString(userId.getBytes(), Base64.NO_WRAP);
    }


}
