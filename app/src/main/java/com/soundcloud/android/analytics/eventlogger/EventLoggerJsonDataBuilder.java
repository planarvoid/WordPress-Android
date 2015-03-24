package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.res.Resources;

import javax.inject.Inject;

public class EventLoggerJsonDataBuilder extends EventLoggerDataBuilder {

    private static final String BOOGALOO_VERSION = "v0.0.0";

    private final JsonTransformer jsonTransformer;
    private final FeatureFlags flags;

    @Inject
    public EventLoggerJsonDataBuilder(Resources resources, ExperimentOperations experimentOperations,
                                      DeviceHelper deviceHelper, AccountOperations accountOperations,
                                      JsonTransformer jsonTransformer, FeatureFlags flags) {
        super(resources, experimentOperations, deviceHelper, accountOperations);
        this.jsonTransformer = jsonTransformer;
        this.flags = flags;
    }

    @Override
    public String build(ScreenEvent event) {
        try {
            return jsonTransformer.toJson(buildBaseEvent(PAGEVIEW_EVENT, event).pageName(event.getScreenTag()));
        } catch (ApiMapperException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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
                .monetizationType(MONETIZATION_TYPE_AUDIO_AD);

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

        if(flags.isEnabled(Flag.EVENTLOGGER_SEARCH_EVENTS)) {
            if (trackSourceInfo.isFromSearchQuery()) {
                SearchQuerySourceInfo searchQuerySourceInfo = trackSourceInfo.getSearchQuerySourceInfo();
                data.queryUrn(searchQuerySourceInfo.getQueryUrn().toString());
                data.queryPosition(String.valueOf(searchQuerySourceInfo.getUpdatedResultPosition(urn)));
            }
        }
        return data;
    }

    @Override
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

    @Override
    public String build(PlaybackErrorEvent event) {
        return transform(buildPlaybackErrorEvent(event));
    }

    @Override
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

    private String transform(EventLoggerEventData data) {
        try {
            return jsonTransformer.toJson(data);
        } catch (ApiMapperException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private EventLoggerEventData buildBaseEvent(String eventName, TrackingEvent event) {
        return buildBaseEvent(eventName, event.getTimeStamp());
    }

    private EventLoggerEventData buildBaseEvent(String eventName, long timestamp) {
        return new EventLoggerEventData(eventName, BOOGALOO_VERSION, appId, getAnonymousId(), getUserUrn(), String.valueOf(timestamp));
    }
}
