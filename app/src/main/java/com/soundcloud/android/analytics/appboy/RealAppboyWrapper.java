package com.soundcloud.android.analytics.appboy;

import com.appboy.Appboy;
import com.appboy.AppboyUser;
import com.appboy.IAppboyEndpointProvider;
import com.appboy.models.outgoing.AppboyProperties;
import com.appboy.models.outgoing.AttributionData;
import com.appboy.ui.inappmessage.AppboyInAppMessageManager;

import android.app.Activity;
import android.net.Uri;
import android.util.Base64;

public class RealAppboyWrapper implements AppboyWrapper {

    private final Appboy appboy;

    public RealAppboyWrapper(Appboy appboy) {
        this.appboy = appboy;
    }

    public void handleRegistration(String token) {
        appboy.registerAppboyPushMessages(token);
    }

    public void setAppboyEndpointProvider(final String authority) {
        Appboy.setAppboyEndpointProvider(new IAppboyEndpointProvider() {
            @Override
            public Uri getApiEndpoint(Uri appboyEndpoint) {
                return appboyEndpoint.buildUpon()
                                     .authority(authority).build();
            }

            @Override
            public Uri getResourceEndpoint(Uri appboyEndpoint) {
                return appboyEndpoint;
            }
        });
    }

    public void setAttribution(String network, String campaign, String adGroup, String creative) {
        AttributionData attributionData = new AttributionData(network, campaign, adGroup, creative);
        appboy.getCurrentUser().setAttributionData(attributionData);
    }

    public boolean openSession(Activity activity) {
        return appboy.openSession(activity);
    }

    public void registerInAppMessageManager(Activity activity) {
        AppboyInAppMessageManager.getInstance().registerInAppMessageManager(activity);
    }

    public boolean closeSession(Activity activity) {
        return appboy.closeSession(activity);
    }

    public void unregisterInAppMessageManager(Activity activity) {
        AppboyInAppMessageManager.getInstance().unregisterInAppMessageManager(activity);
    }

    public AppboyUser changeUser(String userId) {
        return appboy.changeUser(encodeUserId(userId));
    }

    public void requestImmediateDataFlush() {
        appboy.requestImmediateDataFlush();
    }

    public void requestInAppMessageRefresh() {
        appboy.requestInAppMessageRefresh();
    }

    public boolean logCustomEvent(String eventName, AppboyProperties properties) {
        return appboy.logCustomEvent(eventName, properties);
    }

    public boolean logCustomEvent(String eventName) {
        return appboy.logCustomEvent(eventName);
    }

    private String encodeUserId(String userId) {
        return Base64.encodeToString(userId.getBytes(), Base64.NO_WRAP);
    }

}
