package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.StreamNotificationEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;

import android.content.res.Resources;

import javax.inject.Inject;

public class EventLoggerV1JsonDataBuilder {

    private static final String AUDIO_EVENT = "audio";
    private static final String CLICK_EVENT = "click";
    private static final String IMPRESSION_EVENT = "impression";
    private static final String BOOGALOO_VERSION = "v1.4.0";

    private final int appId;
    private final DeviceHelper deviceHelper;
    private final NetworkConnectionHelper connectionHelper;
    private final AccountOperations accountOperations;
    private final FeatureOperations featureOperations;
    private final JsonTransformer jsonTransformer;

    @Inject
    public EventLoggerV1JsonDataBuilder(Resources resources, DeviceHelper deviceHelper,
                                        NetworkConnectionHelper connectionHelper, AccountOperations accountOperations,
                                        JsonTransformer jsonTransformer, FeatureOperations featureOperations) {
        this.connectionHelper = connectionHelper;
        this.accountOperations = accountOperations;
        this.featureOperations = featureOperations;
        this.appId = resources.getInteger(R.integer.app_id);
        this.deviceHelper = deviceHelper;
        this.jsonTransformer = jsonTransformer;
    }

    public String buildForAudioEvent(PlaybackSessionEvent event) {
        return transform(buildAudioEvent(event));
    }

    public String buildForStreamNotification(StreamNotificationEvent event) {
        switch (event.getKind()) {
            case StreamNotificationEvent.KIND_CLICK:
                return transform(buildStreamNotificationClickEvent(event));
            case StreamNotificationEvent.KIND_IMPRESSION:
                return transform(buildStreamNotificationImpressionEvent(event));
            default:
                throw new IllegalStateException("Unexpected StreamNotificationEvent type: " + event);
        }
    }

    private EventLoggerEventData buildAudioEvent(PlaybackSessionEvent event) {
        final Urn urn = event.getTrackUrn();
        EventLoggerEventData data = buildBaseEvent(AUDIO_EVENT, event)
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .playheadPosition(event.getProgress())
                .trackLength(event.getDuration())
                .track(urn)
                .trackOwner(event.getCreatorUrn())
                .localStoragePlayback(event.isOfflineTrack())
                .consumerSubsPlan(featureOperations.getPlan())
                .trigger(getTrigger(event.getTrackSourceInfo()))
                .protocol(event.get(PlaybackSessionEvent.KEY_PROTOCOL))
                .playerType(event.get(PlaybackSessionEvent.PLAYER_TYPE))
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .monetizedObject(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE))
                .promotedBy(event.get(AdTrackingKeys.KEY_PROMOTER_URN));

        TrackSourceInfo trackSourceInfo = event.getTrackSourceInfo();

        if (event.isPlayEvent()) {
            data.action("play");
        } else {
            data.action("pause");
            data.reason(getStopReason(event));
        }

        if (trackSourceInfo.hasSource()) {
            data.source(trackSourceInfo.getSource());
            data.sourceVersion(trackSourceInfo.getSourceVersion());
        }
        if (trackSourceInfo.isFromPlaylist()) {
            data.inPlaylist(trackSourceInfo.getCollectionUrn());
            data.playlistPosition(trackSourceInfo.getPlaylistPosition());
        }

        if (trackSourceInfo.hasReposter()) {
            data.reposter(trackSourceInfo.getReposter());
        }

        if (trackSourceInfo.isFromSearchQuery()) {
            SearchQuerySourceInfo searchQuerySourceInfo = trackSourceInfo.getSearchQuerySourceInfo();
            data.queryUrn(searchQuerySourceInfo.getQueryUrn().toString());
            data.queryPosition(searchQuerySourceInfo.getUpdatedResultPosition(urn));
        }

        if (trackSourceInfo.isFromStation()) {
            data.queryUrn(trackSourceInfo.getCollectionUrn().toString());
        }

        return data;
    }

    private EventLoggerEventData buildStreamNotificationImpressionEvent(StreamNotificationEvent event) {
        return buildBaseEvent(IMPRESSION_EVENT, event)
                .pageName(event.get(StreamNotificationEvent.KEY_PAGE_NAME))
                .impressionCategory(event.get(StreamNotificationEvent.KEY_IMPRESSION_CATEGORY))
                .impressionName(event.get(StreamNotificationEvent.KEY_IMPRESSION_NAME));
    }

    private EventLoggerEventData buildStreamNotificationClickEvent(StreamNotificationEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .pageName(event.get(StreamNotificationEvent.KEY_PAGE_NAME))
                .clickCategory(event.get(StreamNotificationEvent.KEY_CLICK_CATEGORY))
                .clickName(event.get(StreamNotificationEvent.KEY_CLICK_NAME));
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
        return new EventLoggerEventDataV1(eventName, BOOGALOO_VERSION, appId, getAnonymousId(), getUserUrn(),
                timestamp, connectionHelper.getCurrentConnectionType().getValue());
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
}
