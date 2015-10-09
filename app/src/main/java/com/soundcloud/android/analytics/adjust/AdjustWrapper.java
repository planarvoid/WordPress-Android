package com.soundcloud.android.analytics.adjust;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.LogLevel;
import com.adjust.sdk.OnAttributionChangedListener;
import com.soundcloud.android.R;
import com.soundcloud.android.events.AttributionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Inject;

public class AdjustWrapper {
    private final EventBus eventBus;
    private final ApplicationProperties applicationProperties;

    @Inject
    public AdjustWrapper(EventBus eventBus,
                         ApplicationProperties applicationProperties) {
        this.eventBus = eventBus;
        this.applicationProperties = applicationProperties;
    }

    public void onCreate(Context context) {
        Adjust.onCreate(buildAdjustConfig(context));
    }

    /* package */ void onResume() {
        Adjust.onResume();
    }

    /* package */ void onPause() {
        Adjust.onPause();
    }

    private AdjustConfig buildAdjustConfig(Context context) {
        String appToken = context.getString(R.string.adjust_app_token);
        String environment = context.getString(R.string.adjust_environment);
        AdjustConfig config = new AdjustConfig(context, appToken, environment);

        setEventBuffering(config);
        setLogLevel(config);
        setAttributionListener(config);

        return config;
    }

    private void setEventBuffering(AdjustConfig config) {
        config.setEventBufferingEnabled(true);
    }

    private void setLogLevel(AdjustConfig config) {
        LogLevel level = applicationProperties.isDebugBuild() ? LogLevel.INFO : LogLevel.ERROR;
        config.setLogLevel(level);
    }

    private void setAttributionListener(AdjustConfig config) {
        config.setOnAttributionChangedListener(new OnAttributionChangedListener() {
            @Override
            public void onAttributionChanged(AdjustAttribution adjustAttribution) {
                publishAttribution(adjustAttribution);
            }
        });
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
