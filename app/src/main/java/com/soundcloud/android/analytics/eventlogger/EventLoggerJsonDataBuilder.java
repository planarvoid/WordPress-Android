package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.properties.Flag.HOLISTIC_TRACKING;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

import javax.inject.Inject;
import java.util.ArrayList;

public class EventLoggerJsonDataBuilder {

    // event types
    private static final String BOOGALOO_VERSION = "v0.0.0";
    private final int appId;
    protected final DeviceHelper deviceHelper;
    protected final ExperimentOperations experimentOperations;
    protected final AccountOperations accountOperations;
    private final FeatureFlags featureFlags;
    private final JsonTransformer jsonTransformer;
    private static final String EXPERIMENT_VARIANTS_KEY = "part_of_variants";

    @Inject
    EventLoggerJsonDataBuilder(Resources resources, ExperimentOperations experimentOperations,
                               DeviceHelper deviceHelper, AccountOperations accountOperations,
                               JsonTransformer jsonTransformer, FeatureFlags featureFlags) {
        this.accountOperations = accountOperations;
        this.appId = resources.getInteger(R.integer.app_id);
        this.experimentOperations = experimentOperations;
        this.deviceHelper = deviceHelper;
        this.jsonTransformer = jsonTransformer;
        this.featureFlags = featureFlags;
    }

    public String build(AdOverlayTrackingEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(event.eventName().key(), event)
                .adUrn(event.adUrn().toString())
                .externalMedia(event.adArtworkUrl())
                .monetizedObject(event.monetizableTrack().toString());
        if (event.originScreen().isPresent()) {
            eventData.pageName(event.originScreen().get());
        }
        if (event.impressionName().isPresent()) {
            eventData.impressionName(event.impressionName().get().key());
        }
        if (event.impressionObject().isPresent()) {
            eventData.impressionObject(event.impressionObject().get().toString());
        }
        if (event.clickName().isPresent()) {
            eventData.clickName(event.clickName().get());
        }
        if (event.clickObject().isPresent()) {
            eventData.clickObject(event.clickObject().get().toString());
        }
        if (event.clickTarget().isPresent()) {
            eventData.clickTarget(event.clickTarget().get().toString());
        }
        if (event.monetizationType().isPresent()) {
            eventData.monetizationType(event.monetizationType().get().key());
        }

        return transform(eventData);
    }

    public String build(VisualAdImpressionEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(VisualAdImpressionEvent.EVENT_NAME, event)
                .adUrn(event.adUrn())
                .pageName(event.originScreen())
                .impressionName(event.impressionName().key())
                .monetizedObject(event.trackUrn())
                .monetizationType(event.monetizationType().key())
                .externalMedia(event.adArtworkUrl());

        return transform(eventData);
    }

    public String build(PromotedTrackingEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(event.kind().key(), event)
                .adUrn(event.adUrn())
                .pageName(event.originScreen())
                .monetizationType(event.monetizationType());
        if (event.promoterUrn().isPresent()) {
            eventData.promotedBy(event.promoterUrn().get().toString());
        }
        if (event.clickObject().isPresent()) {
            eventData.clickObject(event.clickObject().get().toString());
        }
        if (event.clickTarget().isPresent()) {
            eventData.clickTarget(event.clickTarget().get().toString());
        }
        if (event.clickName().isPresent()) {
            eventData.clickName(event.clickName().get());
        }
        if (event.impressionObject().isPresent()) {
            eventData.impressionObject(event.impressionObject().get().toString());
        }
        if (event.impressionName().isPresent()) {
            eventData.impressionName(event.impressionName().get().key());
        }

        return transform(eventData);
    }

    public String build(PlaybackPerformanceEvent event) {
        return transform(buildPlaybackPerformanceEvent(event));
    }

    private EventLoggerEventData buildPlaybackPerformanceEvent(PlaybackPerformanceEvent event) {
        return buildBaseEvent(event.eventName().key(), event.getTimestamp())
                .latency(event.getMetricValue())
                .protocol(event.getProtocol().getValue())
                .playerType(event.getPlayerType().getValue())
                .type(getPerformanceEventType(event.getMetric()))
                .host(event.getCdnHost())
                .format(event.getFormat())
                .bitrate(String.valueOf(event.getBitrate()))
                .connectionType(event.getConnectionType().getValue());
    }

    public String build(PlaybackErrorEvent event) {
        return transform(buildPlaybackErrorEvent(event));
    }

    public String build(ForegroundEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(ForegroundEvent.EVENT_NAME, event)
                .pageName(event.pageName())
                .referrer(event.referrer());
        if (event.pageUrn().isPresent()) {
            eventData.pageUrn(event.pageUrn().get().toString());
        }
        if (featureFlags.isEnabled(HOLISTIC_TRACKING)) {
            eventData.clientEventId(event.id());
        }

        return transform(eventData);
    }

    private EventLoggerEventData buildPlaybackErrorEvent(PlaybackErrorEvent event) {
        return buildBaseEvent(PlaybackErrorEvent.EVENT_NAME, event.getTimestamp())
                .protocol(event.getProtocol().getValue())
                .os(deviceHelper.getUserAgent())
                .bitrate(String.valueOf(event.getBitrate()))
                .format(event.getFormat())
                .url(event.getCdnHost())
                .errorCode(event.getCategory())
                .connectionType(event.getConnectionType().getValue());
    }

    private String transform(EventLoggerEventData data) {
        try {
            return jsonTransformer.toJson(data);
        } catch (ApiMapperException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private EventLoggerEventData buildBaseEvent(String eventName, TrackingEvent event) {
        return buildBaseEvent(eventName, event.getTimestamp());
    }

    private EventLoggerEventData buildBaseEvent(String eventName, long timestamp) {
        EventLoggerEventData eventData = new EventLoggerEventData(eventName,
                                                                  BOOGALOO_VERSION,
                                                                  appId,
                                                                  getAnonymousId(),
                                                                  getUserUrn(),
                                                                  timestamp);
        addExperiments(eventData);
        return eventData;
    }

    private void addExperiments(EventLoggerEventData eventData) {
        ArrayList<Integer> activeVariants = experimentOperations.getActiveVariants();
        if (activeVariants.size() > 0) {
            eventData.experiment(EXPERIMENT_VARIANTS_KEY, Strings.joinOn(",").join(activeVariants));
        }
    }

    private String getAnonymousId() {
        return deviceHelper.getUdid();
    }

    private String getUserUrn() {
        return accountOperations.getLoggedInUserUrn().toString();
    }

    private String getPerformanceEventType(int type) {
        switch (type) {
            case PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY:
                return "play";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST:
                return "playlist";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER:
                return "buffer";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK:
                return "seek";
            case PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE:
                return "fragmentRate";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_LOAD:
                return "timeToLoadLibrary";
            case PlaybackPerformanceEvent.METRIC_CACHE_USAGE_PERCENT:
                return "cacheUsage";
            case PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS:
                return "uninterruptedPlaytimeMs";
            default:
                throw new IllegalArgumentException("Unexpected metric type " + type);
        }
    }
}
