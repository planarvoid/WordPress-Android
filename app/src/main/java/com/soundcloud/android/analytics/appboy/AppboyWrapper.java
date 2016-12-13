package com.soundcloud.android.analytics.appboy;

import com.appboy.AppboyUser;
import com.appboy.models.outgoing.AppboyProperties;

import android.app.Activity;

public interface AppboyWrapper {
    void handleRegistration(String token);
    void setAppboyEndpointProvider(final String authority);
    void setAttribution(String network, String campaign, String adGroup, String creative);
    boolean openSession(Activity activity);
    void registerInAppMessageManager(Activity activity);
    boolean closeSession(Activity activity);
    void unregisterInAppMessageManager(Activity activity);
    AppboyUser changeUser(String userId);
    void requestImmediateDataFlush();
    void requestInAppMessageRefresh();
    boolean logCustomEvent(String eventName, AppboyProperties properties);
    boolean logCustomEvent(String eventName);
}
