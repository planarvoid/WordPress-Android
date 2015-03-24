package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.res.Resources;

public abstract class EventLoggerDataBuilder {

    protected static final String MONETIZATION_TYPE_AUDIO_AD = "audio_ad";

    // event types
    protected static final String PAGEVIEW_EVENT = "pageview";
    protected static final String CLICK_EVENT = "click";
    protected static final String IMPRESSION_EVENT = "impression";
    protected static final String AUDIO_EVENT = "audio";
    protected static final String AUDIO_PERFORMANCE_EVENT = "audio_performance";
    protected static final String AUDIO_ERROR_EVENT = "audio_error";

    protected final String appId;
    protected final DeviceHelper deviceHelper;
    protected final ExperimentOperations experimentOperations;
    protected final AccountOperations accountOperations;

    public EventLoggerDataBuilder(Resources resources,
                                     ExperimentOperations experimentOperations,
                                     DeviceHelper deviceHelper,
                                     AccountOperations accountOperations) {
        this.accountOperations = accountOperations;
        this.appId = resources.getString(R.string.app_id);
        this.experimentOperations = experimentOperations;
        this.deviceHelper = deviceHelper;
    }

    public abstract String build(AdOverlayTrackingEvent event);

    public abstract String build(ScreenEvent event);

    public abstract String build(VisualAdImpressionEvent event);

    public abstract String build(UIEvent event);

    public abstract String build(PlaybackPerformanceEvent eventData);

    public abstract String build(PlaybackErrorEvent eventData);

    public abstract String build(SearchEvent event);

    public abstract String buildForAudioAdImpression(PlaybackSessionEvent eventData);

    public abstract String buildForAdFinished(PlaybackSessionEvent eventData);

    public abstract String buildForAudioEvent(PlaybackSessionEvent eventData);

    protected String getAnonymousId() {
        return deviceHelper.getUDID();
    }

    protected String getUserUrn() {
        return accountOperations.getLoggedInUserUrn().toString();
    }

    protected String getTrigger(TrackSourceInfo trackSourceInfo) {
        return trackSourceInfo.getIsUserTriggered() ? "manual" : "auto";
    }

    protected String getPerformanceEventType(int type) {
        switch (type) {
            case PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY:
                return "play";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER:
                return "buffer";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST:
                return "playlist";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK:
                return "seek";
            case PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE:
                return "fragmentRate";
            default:
                throw new IllegalArgumentException("Unexpected metric type " + type);
        }
    }

    // EventLogger v0 requires us to pass URNs in the legacy format
    protected String getLegacyTrackUrn(String urn) {
        return urn.replaceFirst(":tracks:", ":sounds:");
    }

}
