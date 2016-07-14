package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.PlayableTrackingKeys;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.res.Resources;

import javax.inject.Inject;
import java.util.Map;

public class EventLoggerJsonDataBuilder {

    private static final String MONETIZATION_TYPE_AUDIO_AD = "audio_ad";
    // event types
    private static final String PAGEVIEW_EVENT = "pageview";
    private static final String CLICK_EVENT = "click";
    private static final String IMPRESSION_EVENT = "impression";
    private static final String FOREGROUND_EVENT = "foreground";
    private static final String AUDIO_PERFORMANCE_EVENT = "audio_performance";
    private static final String AUDIO_ERROR_EVENT = "audio_error";
    private static final String BOOGALOO_VERSION = "v0.0.0";
    protected final int appId;
    protected final DeviceHelper deviceHelper;
    protected final ExperimentOperations experimentOperations;
    protected final AccountOperations accountOperations;

    private final JsonTransformer jsonTransformer;

    @Inject
    public EventLoggerJsonDataBuilder(Resources resources, ExperimentOperations experimentOperations,
                                      DeviceHelper deviceHelper, AccountOperations accountOperations,
                                      JsonTransformer jsonTransformer) {
        this.accountOperations = accountOperations;
        this.appId = resources.getInteger(R.integer.app_id);
        this.experimentOperations = experimentOperations;
        this.deviceHelper = deviceHelper;
        this.jsonTransformer = jsonTransformer;
    }

    public String build(ScreenEvent event) {
        try {
            return jsonTransformer.toJson(buildBaseEvent(PAGEVIEW_EVENT, event)
                                                  .pageName(event.getScreenTag())
                                                  .queryUrn(event.getQueryUrn())
                                                  .pageUrn(event.getPageUrn()));
        } catch (ApiMapperException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String build(UIEvent event) {
        switch (event.getKind()) {
            case UIEvent.KIND_AUDIO_AD_CLICK:
                return transform(getAudioAdClickEvent(event));
            case UIEvent.KIND_SKIP_AUDIO_AD_CLICK:
                return transform(getAudioAdSkipClickEvent(event));
            case UIEvent.KIND_LIKE:
                return transform(getEngagementEvent("like::add", event));
            case UIEvent.KIND_UNLIKE:
                return transform(getEngagementEvent("like::remove", event));
            case UIEvent.KIND_REPOST:
                return transform(getEngagementEvent("repost::add", event));
            case UIEvent.KIND_UNREPOST:
                return transform(getEngagementEvent("repost::remove", event));
            default:
                throw new IllegalStateException("Unexpected UIEvent type: " + event);
        }
    }

    private EventLoggerEventData getEngagementEvent(String clickName, UIEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(CLICK_EVENT, event)
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .monetizationType(event.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE))
                .promotedBy(event.get(PlayableTrackingKeys.KEY_PROMOTER_URN))
                .clickName(clickName)
                .clickObject(event.get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN));

        if (!event.get(PlayableTrackingKeys.KEY_PAGE_URN).equals(Urn.NOT_SET.toString())) {
            eventData.pageUrn(event.get(PlayableTrackingKeys.KEY_PAGE_URN));
        }

        return eventData;
    }

    private EventLoggerEventData getAudioAdSkipClickEvent(UIEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .clickName("ad::skip")
                .clickObject(event.get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN))
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .monetizedObject(event.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(MONETIZATION_TYPE_AUDIO_AD)
                .externalMedia(event.get(PlayableTrackingKeys.KEY_AD_ARTWORK_URL));
    }

    private EventLoggerEventData getAudioAdClickEvent(UIEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .clickName("clickthrough::companion_display")
                .clickTarget(event.get(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL))
                .clickObject(event.get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN))
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .monetizedObject(event.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(MONETIZATION_TYPE_AUDIO_AD)
                .externalMedia(event.get(PlayableTrackingKeys.KEY_AD_ARTWORK_URL));
    }

    public String build(AdOverlayTrackingEvent event) {
        if (event.getKind().equals(AdOverlayTrackingEvent.KIND_CLICK)) {
            return transform(getAdOverlayClickThroughEvent(event));
        } else {
            return transform(getAdOverlayImpressionEvent(event));
        }
    }

    private EventLoggerEventData getAdOverlayImpressionEvent(AdOverlayTrackingEvent event) {
        return buildBaseEvent(IMPRESSION_EVENT, event)
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .externalMedia(event.get(PlayableTrackingKeys.KEY_AD_ARTWORK_URL))
                .impressionName(event.get(PlayableTrackingKeys.KEY_AD_TYPE))
                .impressionObject(event.get(PlayableTrackingKeys.KEY_AD_TRACK_URN))
                .monetizedObject(event.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(event.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE));
    }

    private EventLoggerEventData getAdOverlayClickThroughEvent(AdOverlayTrackingEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .clickTarget(event.get(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL))
                .clickObject(event.get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN))
                .clickName("clickthrough::" + event.get(PlayableTrackingKeys.KEY_AD_TYPE))
                .externalMedia(event.get(PlayableTrackingKeys.KEY_AD_ARTWORK_URL))
                .monetizedObject(event.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(event.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE));
    }

    public String build(VisualAdImpressionEvent event) {
        return transform(getVisualAdImpressionData(event));
    }

    private EventLoggerEventData getVisualAdImpressionData(VisualAdImpressionEvent event) {
        return buildBaseEvent(IMPRESSION_EVENT, event)
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .impressionName("companion_display")
                .impressionObject(event.get(PlayableTrackingKeys.KEY_AD_TRACK_URN))
                .monetizedObject(event.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(MONETIZATION_TYPE_AUDIO_AD)
                .externalMedia(event.get(PlayableTrackingKeys.KEY_AD_ARTWORK_URL));
    }

    public String build(PromotedTrackingEvent event) {
        switch (event.getKind()) {
            case PromotedTrackingEvent.KIND_CLICK:
                return transform(getPromotedClickEvent(event));
            case PromotedTrackingEvent.KIND_IMPRESSION:
                return transform(getPromotedImpressionEvent(event));

            default:
                throw new IllegalStateException("Unexpected PromotedTrackingEvent type: " + event);
        }
    }

    private EventLoggerEventData getPromotedClickEvent(PromotedTrackingEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .monetizationType(event.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE))
                .promotedBy(event.get(PlayableTrackingKeys.KEY_PROMOTER_URN))
                .clickName("item_navigation")
                .clickObject(event.get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN))
                .clickTarget(event.get(PlayableTrackingKeys.KEY_CLICK_TARGET_URN));
    }

    private EventLoggerEventData getPromotedImpressionEvent(PromotedTrackingEvent event) {
        String impressionObject = event.get(PlayableTrackingKeys.KEY_AD_TRACK_URN);
        String impressionName = new Urn(impressionObject).isPlaylist() ? "promoted_playlist" : "promoted_track";
        return buildBaseEvent(IMPRESSION_EVENT, event)
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .monetizationType(event.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE))
                .promotedBy(event.get(PlayableTrackingKeys.KEY_PROMOTER_URN))
                .impressionName(impressionName)
                .impressionObject(impressionObject);
    }

    public String build(PlaybackPerformanceEvent event) {
        return transform(buildPlaybackPerformanceEvent(event));
    }

    private EventLoggerEventData buildPlaybackPerformanceEvent(PlaybackPerformanceEvent event) {
        return buildBaseEvent(AUDIO_PERFORMANCE_EVENT, event.getTimestamp())
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

    public String build(SearchEvent event) {
        switch (event.getKind()) {
            case SearchEvent.KIND_RESULTS:
            case SearchEvent.KIND_SUGGESTION:
                return transform(buildBaseEvent(CLICK_EVENT, event)
                                         .pageName(event.get(SearchEvent.KEY_PAGE_NAME))
                                         .queryUrn(event.get(SearchEvent.KEY_QUERY_URN))
                                         .queryPosition(event.getClickPosition())
                                         .clickName(event.get(SearchEvent.KEY_CLICK_NAME))
                                         .clickObject(event.get(SearchEvent.KEY_CLICK_OBJECT)));

            case SearchEvent.KIND_SUBMIT:
                return transform(buildBaseEvent(CLICK_EVENT, event)
                                         .queryUrn(event.get(SearchEvent.KEY_QUERY_URN))
                                         .clickName(event.get(SearchEvent.KEY_CLICK_NAME)));
            default:
                throw new IllegalArgumentException("Unexpected Search Event type " + event);
        }
    }

    public String build(ForegroundEvent event) {
        switch (event.getKind()) {
            case ForegroundEvent.KIND_OPEN:
                return transform(buildBaseEvent(FOREGROUND_EVENT, event)
                                         .pageName(event.get(ForegroundEvent.KEY_PAGE_NAME))
                                         .pageUrn(event.get(ForegroundEvent.KEY_PAGE_URN))
                                         .referrer(event.get(ForegroundEvent.KEY_REFERRER)));
            default:
                throw new IllegalArgumentException("Unexpected Foreground Event type " + event);
        }
    }

    private EventLoggerEventData buildPlaybackErrorEvent(PlaybackErrorEvent event) {
        return buildBaseEvent(AUDIO_ERROR_EVENT, event.getTimestamp())
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
        for (Map.Entry<String, Integer> pair : experimentOperations.getTrackingParams().entrySet()) {
            eventData.experiment(pair.getKey(), pair.getValue());
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
