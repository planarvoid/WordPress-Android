package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.MidTierTrackEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpsellTrackingEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.res.Resources;

import javax.inject.Inject;

public class EventLoggerJsonDataBuilder {

    private static final String MONETIZATION_TYPE_AUDIO_AD = "audio_ad";
    // event types
    private static final String PAGEVIEW_EVENT = "pageview";
    private static final String CLICK_EVENT = "click";
    private static final String IMPRESSION_EVENT = "impression";
    private static final String AUDIO_EVENT = "audio";
    private static final String FOREGROUND_EVENT = "foreground";
    private static final String AUDIO_PERFORMANCE_EVENT = "audio_performance";
    private static final String AUDIO_ERROR_EVENT = "audio_error";
    private static final String BOOGALOO_VERSION = "v0.0.0";
    protected final String appId;
    protected final DeviceHelper deviceHelper;
    protected final ExperimentOperations experimentOperations;
    protected final AccountOperations accountOperations;

    private final JsonTransformer jsonTransformer;

    @Inject
    public EventLoggerJsonDataBuilder(Resources resources, ExperimentOperations experimentOperations,
                                      DeviceHelper deviceHelper, AccountOperations accountOperations,
                                      JsonTransformer jsonTransformer) {
        this.accountOperations = accountOperations;
        this.appId = resources.getString(R.string.app_id);
        this.experimentOperations = experimentOperations;
        this.deviceHelper = deviceHelper;
        this.jsonTransformer = jsonTransformer;
    }

    public String build(ScreenEvent event) {
        try {
            return jsonTransformer.toJson(buildBaseEvent(PAGEVIEW_EVENT, event).pageName(event.getScreenTag()));
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
            default:
                throw new IllegalStateException("Unexpected UIEvent type: " + event);
        }
    }

    private EventLoggerEventData getAudioAdSkipClickEvent(UIEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .pageName(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN))
                .clickName("ad::skip")
                .clickObject(event.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN))
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .monetizedObject(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(MONETIZATION_TYPE_AUDIO_AD)
                .externalMedia(event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL));
    }

    private EventLoggerEventData getAudioAdClickEvent(UIEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .pageName(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN))
                .clickName("clickthrough::companion_display")
                .clickTarget(event.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL))
                .clickObject(event.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN))
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .monetizedObject(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(MONETIZATION_TYPE_AUDIO_AD)
                .externalMedia(event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL));
    }

    public String build(AdOverlayTrackingEvent event) {
        if (event.getKind().equals(AdOverlayTrackingEvent.KIND_CLICK)) {
            return transform(getAudioAdClickThroughEvent(event));
        } else {
            return transform(getAudioAdImpressionEvent(event));
        }
    }

    private EventLoggerEventData getAudioAdImpressionEvent(AdOverlayTrackingEvent event) {
        return buildBaseEvent(IMPRESSION_EVENT, event)
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .pageName(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN))
                .externalMedia(event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL))
                .impressionName(event.get(AdTrackingKeys.KEY_AD_TYPE))
                .impressionObject(event.get(AdTrackingKeys.KEY_AD_TRACK_URN))
                .monetizedObject(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE));
    }

    private EventLoggerEventData getAudioAdClickThroughEvent(AdOverlayTrackingEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .pageName(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN))
                .clickTarget(event.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL))
                .clickObject(event.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN))
                .clickName("clickthrough::" + event.get(AdTrackingKeys.KEY_AD_TYPE))
                .externalMedia(event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL))
                .monetizedObject(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE));
    }

    public String build(VisualAdImpressionEvent event) {
        return transform(getVisualAdImpressionData(event));
    }

    private EventLoggerEventData getVisualAdImpressionData(VisualAdImpressionEvent event) {
        return buildBaseEvent(IMPRESSION_EVENT, event)
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .pageName(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN))
                .impressionName("companion_display")
                .impressionObject(event.get(AdTrackingKeys.KEY_AD_TRACK_URN))
                .monetizedObject(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(MONETIZATION_TYPE_AUDIO_AD)
                .externalMedia(event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL));
    }

    public String buildForAdFinished(PlaybackSessionEvent event) {
        return transform(buildAudioAdFinishedEvent(event));
    }

    private EventLoggerEventData buildAudioAdFinishedEvent(PlaybackSessionEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .clickObject(event.get(PlaybackSessionEvent.KEY_TRACK_URN))
                .clickName("ad::finish")
                .monetizedObject(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(MONETIZATION_TYPE_AUDIO_AD);
    }

    public String buildForAudioAdImpression(PlaybackSessionEvent event) {
        return transform(buildAudioAdImpressionEvent(event));
    }

    private EventLoggerEventData buildAudioAdImpressionEvent(PlaybackSessionEvent event) {
        return buildBaseEvent(IMPRESSION_EVENT, event)
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .impressionName("audio_ad_impression")
                .impressionObject(event.get(AdTrackingKeys.KEY_AD_TRACK_URN))
                .monetizedObject(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(MONETIZATION_TYPE_AUDIO_AD);
    }

    public String build(MidTierTrackEvent event) {
        switch (event.getKind()) {
            case MidTierTrackEvent.KIND_IMPRESSION:
                return transform(buildBaseEvent(IMPRESSION_EVENT, event)
                        .pageName(event.getPageName())
                        .impressionName("consumer_sub_track")
                        .impressionObject(String.valueOf(event.getTrackUrn())));
            case MidTierTrackEvent.KIND_CLICK:
                return transform(buildBaseEvent(CLICK_EVENT, event)
                        .pageName(event.getPageName())
                        .clickName("consumer_sub_track")
                        .clickObject(String.valueOf(event.getTrackUrn())));
            default:
                throw new IllegalStateException("Unexpected MidTierTrackEvent type: " + event);
        }
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
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .pageName(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN))
                .monetizationType(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE))
                .promotedBy(event.get(AdTrackingKeys.KEY_PROMOTER_URN))
                .clickName("item_navigation")
                .clickObject(event.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN))
                .clickTarget(event.get(AdTrackingKeys.KEY_CLICK_TARGET_URN));
    }

    private EventLoggerEventData getPromotedImpressionEvent(PromotedTrackingEvent event) {
        String impressionObject = event.get(AdTrackingKeys.KEY_AD_TRACK_URN);
        String impressionName = new Urn(impressionObject).isPlaylist() ? "promoted_playlist" : "promoted_track";
        return buildBaseEvent(IMPRESSION_EVENT, event)
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .pageName(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN))
                .monetizationType(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE))
                .promotedBy(event.get(AdTrackingKeys.KEY_PROMOTER_URN))
                .impressionName(impressionName)
                .impressionObject(impressionObject);
    }

    public String buildForAudioEvent(PlaybackSessionEvent event) {
        return transform(buildAudioEvent(event));
    }

    private EventLoggerEventData buildAudioEvent(PlaybackSessionEvent event) {
        final Urn urn = event.getTrackUrn();
        EventLoggerEventData data = buildBaseEvent(AUDIO_EVENT, event)
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(event.getDuration())
                .sound(getLegacyTrackUrn(urn.toString()))
                .trigger(getTrigger(event.getTrackSourceInfo()))
                .protocol(event.get(PlaybackSessionEvent.KEY_PROTOCOL))
                .playerType(event.get(PlaybackSessionEvent.PLAYER_TYPE))
                .connectionType(event.get(PlaybackSessionEvent.CONNECTION_TYPE))
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .monetizedObject(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE))
                .promotedBy(event.get(AdTrackingKeys.KEY_PROMOTER_URN));

        TrackSourceInfo trackSourceInfo = event.getTrackSourceInfo();

        if (event.isPlayEvent()) {
            data.action("play");
        } else {
            data.action("stop");
            data.reason(getStopReason(event));
        }

        if (trackSourceInfo.hasSource()) {
            data.source(trackSourceInfo.getSource());
            data.sourceVersion(trackSourceInfo.getSourceVersion());
        }
        if (trackSourceInfo.isFromPlaylist()) {
            data.playlistId(String.valueOf(trackSourceInfo.getPlaylistUrn().getNumericId()));
            data.playlistPosition(String.valueOf(trackSourceInfo.getPlaylistPosition()));
        }

        if (trackSourceInfo.isFromSearchQuery()) {
            SearchQuerySourceInfo searchQuerySourceInfo = trackSourceInfo.getSearchQuerySourceInfo();
            data.queryUrn(searchQuerySourceInfo.getQueryUrn().toString());
            data.queryPosition(String.valueOf(searchQuerySourceInfo.getUpdatedResultPosition(urn)));
        }
        return data;
    }

    public String build(PlaybackPerformanceEvent event) {
        return transform(buildPlaybackPerformanceEvent(event));
    }

    private EventLoggerEventData buildPlaybackPerformanceEvent(PlaybackPerformanceEvent event) {
        return buildBaseEvent(AUDIO_PERFORMANCE_EVENT, event.getTimeStamp())
                .latency(event.getMetricValue())
                .protocol(event.getProtocol().getValue())
                .playerType(event.getPlayerType().getValue())
                .type(getPerformanceEventType(event.getMetric()))
                .host(event.getCdnHost())
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
                        .queryPosition(event.get(SearchEvent.KEY_CLICK_POSITION))
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
                .bitrate(event.getBitrate())
                .format(event.getFormat())
                .url(event.getCdnHost())
                .errorCode(event.getCategory())
                .connectionType(event.getConnectionType().getValue());
    }

    public String build(UpsellTrackingEvent event) {
        switch (event.getKind()) {
            case UpsellTrackingEvent.KIND_CLICK:
                return transform(buildBaseEvent(CLICK_EVENT, event)
                        .clickName("clickthrough::consumer_sub_ad")
                        .clickObject(event.get(UpsellTrackingEvent.KEY_TCODE)));
            case UpsellTrackingEvent.KIND_IMPRESSION:
                return transform(buildBaseEvent(IMPRESSION_EVENT, event)
                        .impressionName("consumer_sub_ad")
                        .impressionObject(event.get(UpsellTrackingEvent.KEY_TCODE)));
            default:
                throw new IllegalArgumentException("Unexpected upsell tracking event type " + event);
        }
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
        return new EventLoggerEventData(eventName, BOOGALOO_VERSION, appId, getAnonymousId(), getUserUrn(), String.valueOf(timestamp));
    }

    private String getAnonymousId() {
        return deviceHelper.getUdid();
    }

    private String getUserUrn() {
        return accountOperations.getLoggedInUserUrn().toString();
    }

    private String getTrigger(TrackSourceInfo trackSourceInfo) {
        return trackSourceInfo.getIsUserTriggered() ? "manual" : "auto";
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

    private String getStopReason(PlaybackSessionEvent eventData) {
        switch (eventData.getStopReason()) {
            case PlaybackSessionEvent.STOP_REASON_PAUSE:
                return "pause";
            case PlaybackSessionEvent.STOP_REASON_BUFFERING:
                return "buffering";
            case PlaybackSessionEvent.STOP_REASON_SKIP:
                return "skip";
            case PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED:
                return "track_finished";
            case PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE:
                return "end_of_content";
            case PlaybackSessionEvent.STOP_REASON_NEW_QUEUE:
                return "context_change";
            case PlaybackSessionEvent.STOP_REASON_ERROR:
                return "playback_error";
            default:
                throw new IllegalArgumentException("Unexpected stop reason : " + eventData.getStopReason());
        }
    }

    // EventLogger v0 requires us to pass URNs in the legacy format
    private String getLegacyTrackUrn(String urn) {
        return urn.replaceFirst(":tracks:", ":sounds:");
    }

}
