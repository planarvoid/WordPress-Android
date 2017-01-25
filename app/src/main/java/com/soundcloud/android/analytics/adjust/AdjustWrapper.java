package com.soundcloud.android.analytics.adjust;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.adjust.sdk.LogLevel;
import com.soundcloud.android.R;
import com.soundcloud.android.events.AttributionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Inject;

public class AdjustWrapper {

    private final EventBus eventBus;
    private final ApplicationProperties applicationProperties;
    private final FeatureFlags flags;

    @Inject
    AdjustWrapper(EventBus eventBus,
                  ApplicationProperties applicationProperties,
                  FeatureFlags flags) {
        this.eventBus = eventBus;
        this.applicationProperties = applicationProperties;
        this.flags = flags;
    }

    void onCreate(Context context) {
        Adjust.onCreate(buildAdjustConfig(context));
    }

    void onResume() {
        Adjust.onResume();
    }

    void onPause() {
        Adjust.onPause();
    }

    void trackEvent(String token) {
        Adjust.trackEvent(new AdjustEvent(token));
    }

    void trackPurchase(String token, String value, String currency) {
        AdjustEvent purchaseEvent = new AdjustEvent(token);
        purchaseEvent.setRevenue(Double.valueOf(value), currency);
        Adjust.trackEvent(purchaseEvent);
    }

    private AdjustConfig buildAdjustConfig(Context context) {
        String appToken = context.getString(R.string.adjust_app_token);
        String environment = context.getString(R.string.adjust_environment);
        AdjustConfig config = new AdjustConfig(context, appToken, environment);

        setEventBuffering(config);
        setLogLevel(config);
        setAttributionListener(config);

        if (flags.isEnabled(Flag.ADJUST_DEFERRED_DEEPLINKS)) {
            config.setOnDeeplinkResponseListener(deeplink -> true);
        }

        return config;
    }

    private void setEventBuffering(AdjustConfig config) {
        config.setEventBufferingEnabled(true);
    }

    private void setLogLevel(AdjustConfig config) {
        LogLevel level = applicationProperties.isDevelopmentMode() ? LogLevel.INFO : LogLevel.ERROR;
        config.setLogLevel(level);
    }

    private void setAttributionListener(AdjustConfig config) {
        config.setOnAttributionChangedListener(adjustAttribution -> publishAttribution(adjustAttribution));
    }

    private void publishAttribution(AdjustAttribution adjustAttribution) {
        eventBus.publish(EventQueue.TRACKING, new AttributionEvent(
                adjustAttribution.network,
                adjustAttribution.campaign,
                adjustAttribution.adgroup,
                adjustAttribution.creative
        ));
    }

}
