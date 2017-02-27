package com.soundcloud.android.di.testimplementations;

import com.appboy.AppboyUser;
import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;

import android.app.Activity;

// Empty implementation for acceptance tests
public class NoopAppboyWrapper implements AppboyWrapper {

    @Override
    public void handleRegistration(String token) {

    }

    @Override
    public void setAppboyEndpointProvider(String authority) {

    }

    @Override
    public void setAttribution(String network, String campaign, String adGroup, String creative) {

    }

    @Override
    public boolean openSession(Activity activity) {
        return false;
    }

    @Override
    public void registerInAppMessageManager(Activity activity) {

    }

    @Override
    public boolean closeSession(Activity activity) {
        return false;
    }

    @Override
    public void unregisterInAppMessageManager(Activity activity) {

    }

    @Override
    public AppboyUser changeUser(String userId) {
        return null;
    }

    @Override
    public void requestImmediateDataFlush() {

    }

    @Override
    public void requestInAppMessageRefresh() {

    }

    @Override
    public boolean logCustomEvent(String eventName, AppboyProperties properties) {
        return false;
    }

    @Override
    public boolean logCustomEvent(String eventName) {
        return false;
    }
}
