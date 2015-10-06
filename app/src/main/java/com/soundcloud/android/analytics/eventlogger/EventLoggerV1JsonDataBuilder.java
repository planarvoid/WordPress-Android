package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.OfflineSyncTrackingEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;

import javax.inject.Inject;
import java.util.Map;

public class EventLoggerV1JsonDataBuilder {

    private static final String AUDIO_EVENT = "audio";
    private static final String CLICK_EVENT = "click";
    private static final String OFFLINE_SYNC_EVENT = "offline_sync";
    private static final String IMPRESSION_EVENT = "impression";
    private static final String BOOGALOO_VERSION = "v1.4.0";

    private final int appId;
    private final DeviceHelper deviceHelper;
    private final NetworkConnectionHelper connectionHelper;
    private final AccountOperations accountOperations;
    private final FeatureOperations featureOperations;
    private final ExperimentOperations experimentOperations;
    private final JsonTransformer jsonTransformer;

    @Inject
    public EventLoggerV1JsonDataBuilder(Resources resources, DeviceHelper deviceHelper,
                                        NetworkConnectionHelper connectionHelper, AccountOperations accountOperations,
                                        JsonTransformer jsonTransformer, FeatureOperations featureOperations,
                                        ExperimentOperations experimentOperations) {
        this.connectionHelper = connectionHelper;
        this.accountOperations = accountOperations;
        this.featureOperations = featureOperations;
        this.experimentOperations = experimentOperations;
        this.appId = resources.getInteger(R.integer.app_id);
        this.deviceHelper = deviceHelper;
        this.jsonTransformer = jsonTransformer;
    }

    public String buildForAudioEvent(PlaybackSessionEvent event) {
        return transform(buildAudioEvent(event));
    }

    public String buildForFacebookInvites(FacebookInvitesEvent event) {
        switch (event.getKind()) {
            case FacebookInvitesEvent.KIND_CLICK:
                return transform(buildFacebookInvitesClickEvent(event));
            case FacebookInvitesEvent.KIND_IMPRESSION:
                return transform(buildFacebookInvitesImpressionEvent(event));
            default:
                throw new IllegalStateException("Unexpected FacebookInvitesEvent type: " + event);
        }
    }

    public String buildForUIEvent(UIEvent event) {
        switch (event.getKind()) {
            case UIEvent.KIND_OFFLINE_PLAYLIST_ADD:
                return transform(buildClickEvent("playlist_to_offline::add", event));
            case UIEvent.KIND_OFFLINE_PLAYLIST_REMOVE:
                return transform(buildClickEvent("playlist_to_offline::remove", event));
            case UIEvent.KIND_OFFLINE_LIKES_ADD:
                return transform(buildClickEvent("likes_to_offline::add", event));
            case UIEvent.KIND_OFFLINE_LIKES_REMOVE:
                return transform(buildClickEvent("likes_to_offline::remove", event));
            case UIEvent.KIND_OFFLINE_COLLECTION_ADD:
                return transform(buildClickEvent("collection_to_offline::add", event));
            case UIEvent.KIND_OFFLINE_COLLECTION_REMOVE:
                return transform(buildClickEvent("collection_to_offline::remove", event));
            case UIEvent.KIND_SHARE:
                return transform(buildEngagementEvent("share", event));
            case UIEvent.KIND_REPOST:
                return transform(buildEngagementEvent("repost::add", event));
            case UIEvent.KIND_UNREPOST:
                return transform(buildEngagementEvent("repost::remove", event));
            case UIEvent.KIND_LIKE:
                return transform(buildEngagementEvent("like::add", event));
            case UIEvent.KIND_UNLIKE:
                return transform(buildEngagementEvent("like::remove", event));
            default:
                throw new IllegalStateException("Unexpected UIEvent type: " + event);
        }
    }

    public String buildForOfflineSyncEvent(OfflineSyncTrackingEvent event) {
        final EventLoggerEventData eventLoggerEventData = buildBaseEvent(OFFLINE_SYNC_EVENT, event)
                .eventStage(event.getKind())
                .track(event.getTrackUrn())
                .trackOwner(event.getTrackOwner())
                .inOfflineLikes(event.isFromLikes())
                .inOfflinePlaylist(event.partOfPlaylist())
                .appVersion(deviceHelper.getAppVersion());
        return transform(eventLoggerEventData);
    }

    public String buildForCollectionEvent(CollectionEvent event) {
        switch (event.getKind()) {
            case CollectionEvent.KIND_SET:
                return transform(buildCollectionEvent("filter_sort::set", event));
            case CollectionEvent.KIND_CLEAR:
                return transform(buildCollectionEvent("filter_sort::clear", event));
            default:
                throw new IllegalStateException("Unexpected CollectionEvent type: " + event);
        }
    }

    private EventLoggerEventData buildCollectionEvent(String clickName, CollectionEvent event) {
        return buildBaseEvent("click", event)
                .clickName(clickName)
                .pageName(Screen.COLLECTIONS.get())
                .clickCategory(EventLoggerClickCategories.COLLECTION)
                .clickObject(event.get(CollectionEvent.KEY_OBJECT))
                .clickTarget(event.get(CollectionEvent.KEY_TARGET));
    }

    private EventLoggerEventData buildClickEvent(String clickName, UIEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .clickName(clickName)
                .pageName(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN))
                .adUrn(event.get(AdTrackingKeys.KEY_AD_URN))
                .monetizationType(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE))
                .promotedBy(event.get(AdTrackingKeys.KEY_PROMOTER_URN))
                .clickObject(event.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN));
    }

    private EventLoggerEventData buildEngagementEvent(String engagementClickName, UIEvent event) {
        EventLoggerEventData eventData = buildClickEvent(engagementClickName, event)
                .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                .clickSource(event.getClickSource());

        final Optional<Urn> sourceUrn = event.getClickSourceUrn();
        final Optional<Urn> queryUrn = event.getQueryUrn();

        if (sourceUrn.isPresent() && !sourceUrn.get().equals(Urn.NOT_SET)) {
            eventData.clickSourceUrn(sourceUrn.get().toString());
        }

        if (queryUrn.isPresent()) {
            eventData.queryUrn(queryUrn.get().toString());
        }

        if (!event.get(AdTrackingKeys.KEY_PAGE_URN).equals(Urn.NOT_SET.toString())) {
            eventData.pageUrn(event.get(AdTrackingKeys.KEY_PAGE_URN));
        }

        if (event.isFromOverflow()) {
            eventData.fromOverflowMenu(event.isFromOverflow());
        }

        return eventData;
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
                .policy(event.get(PlaybackSessionEvent.KEY_POLICY))
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
            data.inOfflinePlaylist(trackSourceInfo.getCollectionUrn());
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
            // When updating it, please update V0 too. Your friend.
            data.sourceUrn(trackSourceInfo.getCollectionUrn().toString());

            if (!trackSourceInfo.getStationsSourceInfo().getQueryUrn().equals(Urn.NOT_SET)) {
                data.queryUrn(trackSourceInfo.getStationsSourceInfo().getQueryUrn().toString());
            }
        }

        return data;
    }

    private EventLoggerEventData buildFacebookInvitesImpressionEvent(FacebookInvitesEvent event) {
        return buildBaseEvent(IMPRESSION_EVENT, event)
                .pageName(event.get(FacebookInvitesEvent.KEY_PAGE_NAME))
                .impressionCategory(event.get(FacebookInvitesEvent.KEY_IMPRESSION_CATEGORY))
                .impressionName(event.get(FacebookInvitesEvent.KEY_IMPRESSION_NAME));
    }

    private EventLoggerEventData buildFacebookInvitesClickEvent(FacebookInvitesEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .pageName(event.get(FacebookInvitesEvent.KEY_PAGE_NAME))
                .clickCategory(event.get(FacebookInvitesEvent.KEY_CLICK_CATEGORY))
                .clickName(event.get(FacebookInvitesEvent.KEY_CLICK_NAME));
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
        EventLoggerEventDataV1 eventData = new EventLoggerEventDataV1(eventName, BOOGALOO_VERSION, appId, getAnonymousId(), getUserUrn(),
                timestamp, connectionHelper.getCurrentConnectionType().getValue());
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

    private String getTrigger(TrackSourceInfo trackSourceInfo) {
        return trackSourceInfo.getIsUserTriggered() ? "manual" : "auto";
    }

    private String getStopReason(PlaybackSessionEvent eventData) {
        switch (eventData.getStopReason()) {
            case PlaybackSessionEvent.STOP_REASON_PAUSE:
                return "pause";
            case PlaybackSessionEvent.STOP_REASON_BUFFERING:
                return "buffer_underrun";
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
            case PlaybackSessionEvent.STOP_REASON_CONCURRENT_STREAMING:
                return "concurrent_streaming";
            default:
                throw new IllegalArgumentException("Unexpected stop reason : " + eventData.getStopReason());
        }
    }
}
