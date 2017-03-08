package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.events.OfflinePerformanceEvent.EVENT_NAME;
import static com.soundcloud.android.properties.Flag.HOLISTIC_TRACKING;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.discovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdPlaybackErrorEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.events.AdRichMediaSessionEvent;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.ScrollDepthEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;

class EventLoggerV1JsonDataBuilder {

    private static final String CLICK_EVENT = "click";
    private static final String INTERACTION_EVENT = "item_interaction";

    // Ads specific events
    private static final String EXPERIMENT_VARIANTS_KEY = "part_of_variants";

    private static final String BOOGALOO_VERSION = "v1.26.0";

    private final int appId;
    private final DeviceHelper deviceHelper;
    private final NetworkConnectionHelper connectionHelper;
    private final AccountOperations accountOperations;
    private final FeatureOperations featureOperations;
    private final ExperimentOperations experimentOperations;
    private final JsonTransformer jsonTransformer;
    private final FeatureFlags featureFlags;

    @Inject
    EventLoggerV1JsonDataBuilder(Resources resources, DeviceHelper deviceHelper,
                                 NetworkConnectionHelper connectionHelper, AccountOperations accountOperations,
                                 JsonTransformer jsonTransformer, FeatureOperations featureOperations,
                                 ExperimentOperations experimentOperations,
                                 FeatureFlags featureFlags) {
        this.connectionHelper = connectionHelper;
        this.accountOperations = accountOperations;
        this.featureOperations = featureOperations;
        this.experimentOperations = experimentOperations;
        this.appId = resources.getInteger(R.integer.app_id);
        this.deviceHelper = deviceHelper;
        this.jsonTransformer = jsonTransformer;
        this.featureFlags = featureFlags;
    }

    String buildForAudioEvent(PlaybackSessionEvent event) {
        return transform(buildAudioEvent(event));
    }

    String buildForScrollDepthEvent(ScrollDepthEvent event) {
        final EventLoggerEventData data = buildBaseEvent(ScrollDepthEvent.KIND, event)
                .clientEventId(event.id())
                .pageName(event.screen().get())
                .action(event.action().get())
                .columnCount(event.columnCount())
                .referringEvent(event.referringEvent());

        if (!event.earliestItems().isEmpty()) {
            data.itemDetails(EventLoggerParam.EARLIEST_ITEM, event.earliestItem());
        }

        if (!event.latestItems().isEmpty()) {
            data.itemDetails(EventLoggerParam.LATEST_ITEM, event.latestItem());
        }

        return transform(data);
    }

    String buildForAdRequest(AdRequestEvent event) {
        EventLoggerEventData data = buildBaseEvent(AdRequestEvent.EVENT_NAME, event)
                .clientEventId(event.id())
                .playerVisible(event.playerVisible())
                .inForeground(event.inForeground())
                .adsRequestSuccess(event.adsRequestSuccess())
                .adsEndpoint(event.adsEndpoint());

        if (event.monetizableTrackUrn().isPresent()) {
            data.monetizedObject(event.monetizableTrackUrn().get().toString());
        }
        if (event.adsReceived().isPresent()) {
            data.adsReceived(mapToJson(event.adsReceived().get().ads));
        }

        return transform(data);
    }

    String buildForStreamAd(InlayAdImpressionEvent event) {
        return transform(buildBaseEvent(InlayAdImpressionEvent.eventName, event.getTimestamp())
                                 .clientEventId(event.id())
                                 .impressionName(InlayAdImpressionEvent.impressionName)
                                 .adUrn(event.ad().toString())
                                 .pageName(InlayAdImpressionEvent.pageName)
                                 .contextPosition(event.contextPosition())
                                 .monetizationType(InlayAdImpressionEvent.monetizationType));
    }

    String buildForAdDelivery(AdDeliveryEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(AdDeliveryEvent.EVENT_NAME, event)
                .clientEventId(event.id())
                .adRequestId(event.adRequestId())
                .playerVisible(event.playerVisible())
                .inForeground(event.inForeground())
                .adDelivered(event.adUrn().toString());
        if (event.monetizableUrn().isPresent()) {
            eventData.monetizedObject(event.monetizableUrn().get().toString());
        }
        return transform(eventData);
    }

    String buildForAdPlaybackSessionEvent(AdPlaybackSessionEvent eventData) {
        final String eventName = eventData.eventName().get().key();
        final EventLoggerEventData data = buildBaseEvent(eventName, eventData).adUrn(eventData.adUrn().toString())
                                                                              .pageName(eventData.pageName())
                                                                              .monetizationType(eventData.monetizationType().key());
        if (eventData.monetizableTrackUrn().isPresent()) {
            data.monetizedObject(eventData.monetizableTrackUrn().get().toString());
        }
        if (eventData.clickName().isPresent()) {
            data.clickName(eventData.clickName().get().key());
        }
        if (eventData.impressionName().isPresent()) {
            data.impressionName(eventData.impressionName().get().key());
        }
        return transform(data);
    }

    String buildForRichMediaSessionEvent(AdRichMediaSessionEvent eventData) {
        EventLoggerEventData data = buildBaseEvent(eventData.eventName(), eventData)
                .adUrn(eventData.adUrn().toString())
                .monetizationType(eventData.monetizationType().key())
                .pageName(eventData.pageName())
                .playheadPosition(eventData.playheadPosition())
                .clientEventId(eventData.clickEventId())
                .trigger(eventData.trigger().key())
                .protocol(eventData.protocol())
                .playerType(eventData.playerType())
                .trackLength(eventData.trackLength());

        data.action(eventData.action().key());

        if (eventData.monetizableTrackUrn().isPresent()) {
            data.monetizedObject(eventData.monetizableTrackUrn().get().toString());
        }
        if (eventData.stopReason().isPresent()) {
            data.reason(eventData.stopReason().get().key());
        }
        if (eventData.source().isPresent()) {
            data.source(eventData.source().get());
        }
        if (eventData.sourceVersion().isPresent()) {
            data.sourceVersion(eventData.sourceVersion().get());
        }
        if (eventData.inPlaylist().isPresent() && !eventData.inPlaylist().get().isLocal()) {
            data.inPlaylist(eventData.inPlaylist().get());

            if (eventData.playlistPosition().isPresent()) {
                data.playlistPosition(eventData.playlistPosition().get());
            }
        }

        if (eventData.reposter().isPresent()) {
            data.reposter(eventData.reposter().get());
        }
        if (eventData.queryUrn().isPresent()) {
            data.queryUrn(eventData.queryUrn().get().toString());
        }
        if (eventData.queryPosition().isPresent()) {
            data.queryPosition(eventData.queryPosition().get());
        }
        if (eventData.sourceUrn().isPresent()) {
            data.sourceUrn(eventData.sourceUrn().get().toString());
        }

        return transform(data);
    }

    String buildForRichMediaPerformance(PlaybackPerformanceEvent event) {
        return transform(buildBaseEvent(event.eventName().key(), event.getTimestamp())
                                 .mediaType(event.isVideoAd() ? "video" : "audio")
                                 .protocol(event.getProtocol().getValue())
                                 .playerType(event.getPlayerType().getValue())
                                 .format(getRichMediaFormatName(event.getFormat()))
                                 .bitrate(event.getBitrate())
                                 .metric(getRichMediaPerformanceEventType(event.getMetric()), event.getMetricValue())
                                 .host(event.getCdnHost()));
    }

    String buildForScreenEvent(ScreenEvent event) {
        try {
            final EventLoggerEventData eventData = buildBaseEvent(ScreenEvent.EVENT_NAME, event)
                    .pageName(event.screen());
            if (event.queryUrn().isPresent()) {
                eventData.queryUrn(event.queryUrn().get().toString());
            }
            if (event.pageUrn().isPresent()) {
                eventData.pageUrn(event.pageUrn().get().toString());
            }
            if (featureFlags.isEnabled(HOLISTIC_TRACKING)) {
                if (event.referringEvent().isPresent()) {
                    eventData.referringEvent(event.referringEvent().get().getId(), event.referringEvent().get().getKind());
                }
                eventData.clientEventId(event.id());
            }

            return jsonTransformer.toJson(eventData);
        } catch (ApiMapperException e) {
            throw new IllegalArgumentException(e);
        }
    }

    String buildForRichMediaErrorEvent(AdPlaybackErrorEvent eventData) {
        final EventLoggerEventData data = buildBaseEvent(AdPlaybackErrorEvent.EVENT_NAME, eventData)
                .mediaType(eventData.mediaType())
                .format(eventData.format())
                .bitrate(eventData.bitrate())
                .errorName(eventData.errorName())
                .host(eventData.host());
        if (eventData.protocol().isPresent()) {
            data.protocol(eventData.protocol().get());
        }
        if (eventData.playerType().isPresent()) {
            data.playerType(eventData.playerType().get());
        }
        return transform(data);
    }

    private String getRichMediaFormatName(String format) {
        switch (format) {
            case PlaybackConstants.MIME_TYPE_MP4:
                return "mp4";
            default:
                return format;
        }
    }

    private String getRichMediaPerformanceEventType(int metric) {
        switch (metric) {
            case PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY:
                return "timeToPlayMs";
            case PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS:
                return "uninterruptedPlaytimeMs";
            default:
                throw new IllegalArgumentException("Unexpected metric type " + metric);
        }
    }

    String buildForFacebookInvites(FacebookInvitesEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(event.eventName().key(), event).pageName(event.pageName());
        if (event.clickName().isPresent()) {
            eventData.clickName(event.clickName().get().key());
        }
        if (event.impressionName().isPresent()) {
            eventData.impressionName(event.impressionName().get().key());
        }
        if (event.clickCategory().isPresent()) {
            eventData.clickCategory(event.clickCategory().get());
        }
        if (event.impressionCategory().isPresent()) {
            eventData.impressionCategory(event.impressionCategory().get());
        }
        return transform(eventData);
    }

    String buildForUpsell(UpgradeFunnelEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(event.eventName().key(), event);

        if (event.pageName().isPresent()) {
            eventData.pageName(event.pageName().get());
        }

        if (event.pageUrn().isPresent()) {
            eventData.pageUrn(event.pageUrn().get());
        }

        if (event.clickName().isPresent()) {
            eventData.clickName(event.clickName().get().key());
        }

        if (event.clickCategory().isPresent()) {
            eventData.clickCategory(event.clickCategory().get().key());
        }

        if (event.clickObject().isPresent()) {
            eventData.clickObject(event.clickObject().get());
        }

        if (event.impressionName().isPresent()) {
            eventData.impressionName(event.impressionName().get().key());
        }

        if (event.impressionCategory().isPresent()) {
            eventData.impressionCategory(event.impressionCategory().get());
        }

        if (event.impressionObject().isPresent()) {
            eventData.impressionObject(event.impressionObject().get());
        }

        return transform(eventData);
    }

    String buildForUIEvent(UIEvent event) {
        switch (event.kind()) {
            case SHARE:
            case REPOST:
            case UNREPOST:
            case LIKE:
            case UNLIKE:
            case SHUFFLE:
            case SWIPE_SKIP:
            case SYSTEM_SKIP:
            case BUTTON_SKIP:
            case VIDEO_AD_FULLSCREEN:
            case VIDEO_AD_SHRINK:
            case VIDEO_AD_MUTE:
            case VIDEO_AD_UNMUTE:
            case AD_CLICKTHROUGH:
            case SKIP_AD_CLICK:
            case FOLLOW:
            case UNFOLLOW:
            case PLAYER_OPEN:
            case PLAYER_CLOSE:
            case PLAY_QUEUE_OPEN:
            case PLAY_QUEUE_CLOSE:
            case PLAY_QUEUE_SHUFFLE:
            case PLAY_QUEUE_TRACK_REORDER:
            case PLAY_QUEUE_TRACK_REMOVE:
            case PLAY_QUEUE_TRACK_REMOVE_UNDO:
            case PLAY_QUEUE_REPEAT:
            case PLAY_NEXT:
                return transform(buildClickEvent(event));
            case RECOMMENDED_PLAYLISTS:
            case MORE_PLAYLISTS_BY_USER:
                return transform(buildItemNavigationClickEvent(event));
            case NAVIGATION:
                return transform(buildInteractionEvent(event));
            default:
                throw new IllegalStateException("Unexpected UIEvent type: " + event);
        }
    }

    String buildForSearchEvent(SearchEvent event) {
        return transform(buildSearchClickEvent(event));
    }

    private EventLoggerEventData buildSearchClickEvent(SearchEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(SearchEvent.EVENT_NAME, event);
        if (event.clickName().isPresent()) {
            eventData.clickName(event.clickName().get().key);
        }
        if (event.clickObject().isPresent()) {
            eventData.clickObject(event.clickObject().get().toString());
        }
        if (event.clickSource().isPresent()) {
            eventData.clickSource(event.clickSource().get().key);
        }
        if (event.pageName().isPresent()) {
            eventData.pageName(event.pageName().get());
        }
        if (event.query().isPresent()) {
            eventData.searchQuery(event.query().get());
        }
        if (event.queryUrn().isPresent()) {
            eventData.queryUrn(event.queryUrn().get().toString());
        }
        if (event.queryPosition().isPresent()) {
            eventData.queryPosition(event.queryPosition().get());
        }
        return eventData;
    }

    boolean isInteractionEvent(UIEvent event) {
        switch (event.kind()) {
            case SHARE:
            case REPOST:
            case UNREPOST:
            case LIKE:
            case UNLIKE:
            case FOLLOW:
            case UNFOLLOW:
                return true;
            default:
                return false;
        }
    }

    String buildForInteractionEvent(UIEvent event) {
        return transform(buildInteractionEvent(event));
    }

    String buildForOfflineInteractionEvent(OfflineInteractionEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(event.eventName().key(), event);
        if (event.pageName().isPresent()) {
            eventData.pageName(event.pageName().get());
        }
        if (event.clickCategory().isPresent()) {
            eventData.clickCategory(event.clickCategory().get());
        }
        if (event.impressionCategory().isPresent()) {
            eventData.impressionCategory(event.impressionCategory().get());
        }
        if (event.clickName().isPresent()) {
            eventData.clickName(event.clickName().get().key());
        }
        if (event.clickObject().isPresent()) {
            eventData.clickObject(event.clickObject().get().toString());
        }
        if (event.impressionName().isPresent()) {
            eventData.impressionName(event.impressionName().get().key());
        }
        if (event.adUrn().isPresent()) {
            eventData.adUrn(event.adUrn().get());
        }
        if (event.monetizationType().isPresent()) {
            eventData.monetizationType(event.monetizationType().get().key());
        }
        if (event.promoterUrn().isPresent()) {
            eventData.promotedBy(event.promoterUrn().get().toString());
        }
        return transform(eventData);
    }

    String buildForOfflinePerformanceEvent(OfflinePerformanceEvent event) {
        final EventLoggerEventData eventLoggerEventData = buildBaseEvent(EVENT_NAME, event)
                .eventStage(event.kind().key())
                .track(event.trackUrn())
                .trackOwner(event.trackOwner())
                .inOfflineLikes(event.isFromLikes())
                .inOfflinePlaylist(event.partOfPlaylist());
        return transform(eventLoggerEventData);
    }

    String buildForCollectionEvent(CollectionEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(CollectionEvent.EVENT_NAME, event).clickName(event.clickName().key()).pageName(event.pageName());
        if (event.source().isPresent()) {
            eventData.clickSource(event.source().get().value());
        }
        if (event.object().isPresent()) {
            eventData.clickObject(event.object().get());
        }
        if (event.clickCategory().isPresent()) {
            eventData.clickCategory(event.clickCategory().get());
        }
        if (event.target().isPresent()) {
            eventData.clickTarget(event.target().get().key());
        }

        return transform(eventData);
    }

    private EventLoggerEventData buildItemNavigationClickEvent(UIEvent event) {
        final EventLoggerEventData eventData = buildClickEvent(event);

        if (event.clickSource().isPresent()) {
            eventData.clickSource(event.clickSource().get());
            eventData.source(event.clickSource().get());
        }

        return eventData;
    }

    private EventLoggerEventData buildClickEvent(UIEvent event) {
        final EventLoggerEventData eventData = buildBaseEvent(CLICK_EVENT, event);
        if (event.clickCategory().isPresent()) {
            eventData.clickCategory(event.clickCategory().get().key());
        }
        if (event.originScreen().isPresent()) {
            eventData.pageName(event.originScreen().get());
        }
        if (event.adUrn().isPresent()) {
            eventData.adUrn(event.adUrn().get());
        }
        if (event.monetizationType().isPresent()) {
            eventData.monetizationType(event.monetizationType().get().key());
        }
        if (event.monetizableTrackUrn().isPresent()) {
            eventData.monetizedObject(event.monetizableTrackUrn().get().toString());
        }
        final Optional<Urn> clickObjectFromEvent = getClickObjectFromEvent(event);
        if (clickObjectFromEvent.isPresent()) {
            eventData.clickObject(clickObjectFromEvent.get().toString());
        }
        if (event.trigger().isPresent()) {
            eventData.clickTrigger(event.trigger().get().key());
        }
        if (event.promoterUrn().isPresent()) {
            eventData.promotedBy(event.promoterUrn().get().toString());
        }
        if (event.clickSource().isPresent()) {
            eventData.clickSource(event.clickSource().get());
        }
        final Optional<Urn> sourceUrn = event.clickSourceUrn();
        final Optional<Urn> queryUrn = event.queryUrn();
        final Optional<Integer> queryPosition = event.queryPosition();

        if (sourceUrn.isPresent() && !sourceUrn.get().equals(Urn.NOT_SET)) {
            eventData.clickSourceUrn(sourceUrn.get().toString());
        }

        if (queryUrn.isPresent() && !queryUrn.get().equals(Urn.NOT_SET)) {
            eventData.queryUrn(queryUrn.get().toString());
        }

        if (queryPosition.isPresent()) {
            eventData.queryPosition(queryPosition.get());
        }

        final Optional<Urn> pageUrn = event.pageUrn();
        if (pageUrn.isPresent() && !pageUrn.get().equals(Urn.NOT_SET)) {
            eventData.pageUrn(pageUrn.get().toString());
        }

        if (event.isFromOverflow().isPresent() && event.isFromOverflow().get()) {
            eventData.fromOverflowMenu(event.isFromOverflow().get());
        }
        if (event.playQueueRepeatMode().isPresent()) {
            eventData.clickRepeat(event.playQueueRepeatMode().get());
        }
        final Optional<Urn> urnOptional = event.pageUrn();

        if (urnOptional.isPresent() && !urnOptional.get().equals(Urn.NOT_SET)) {
            eventData.pageUrn(urnOptional.get().toString());
        }
        if (event.clickthroughsUrl().isPresent()) {
            eventData.clickTarget(event.clickthroughsUrl().get());
        }
        if (event.clickthroughsKind().isPresent()) {
            eventData.clickName(event.clickthroughsKind().get());
        }
        if (event.clickName().isPresent()) {
            eventData.clickName(event.clickName().get().key());
        }
        if (event.shareLinkType().isPresent()) {
            eventData.shareLinkType(event.shareLinkType().get().key());
        }
        return eventData;
    }

    private EventLoggerEventData buildInteractionEvent(UIEvent event) {
        final Optional<AttributingActivity> attributingActivity = event.attributingActivity();
        final Optional<Module> module = event.module();
        final Optional<Urn> pageUrn = event.pageUrn();
        final Optional<Urn> clickObjectFromEvent = getClickObjectFromEvent(event);

        final EventLoggerEventData eventData = buildBaseEvent(INTERACTION_EVENT, event).clientEventId(event.id());
        if (event.action().isPresent()) {
            eventData.action(event.action().get().key());

            if (event.action().get().equals(UIEvent.Action.NAVIGATION) && event.linkType().isPresent()) {
                eventData.linkType(event.linkType().get());
            }
        }

        if (clickObjectFromEvent.isPresent()) {
            eventData.item(clickObjectFromEvent.get().toString());
        }

        if (event.originScreen().isPresent()) {
            eventData.pageName(event.originScreen().get());
        }

        if (event.referringEvent().isPresent()) {
            eventData.pageviewId(event.referringEvent().get().getId());
        }

        if (pageUrn.isPresent() && !pageUrn.get().equals(Urn.NOT_SET)) {
            eventData.pageUrn(pageUrn.get().toString());
        }

        if (attributingActivity.isPresent() && attributingActivity.get().isActive(module)) {
            eventData.attributingActivity(attributingActivity.get().getType(), attributingActivity.get().getResource());
        }

        if (module.isPresent()) {
            eventData.module(module.get().getName());
            eventData.modulePosition(module.get().getPosition());
        }

        if (event.queryUrn().isPresent()) {
            eventData.queryUrn(event.queryUrn().get().toString());
        }

        if (event.queryPosition().isPresent()) {
            eventData.queryPosition(event.queryPosition().get());
        }

        return eventData;
    }

    private Optional<Urn> getClickObjectFromEvent(UIEvent event) {
        return event.clickObjectUrn().isPresent()
               ? event.clickObjectUrn()
               : event.creatorUrn();
    }

    private EventLoggerEventData buildAudioEvent(PlaybackSessionEvent event) {
        final Urn urn = event.trackUrn();
        EventLoggerEventData data = buildBaseEvent(PlaybackSessionEvent.EVENT_NAME, event)
                .action(event.kind().key())
                .pageName(event.trackSourceInfo().getOriginScreen())
                .playheadPosition(event.progress())
                .trackLength(event.duration())
                .track(urn)
                .trackOwner(event.creatorUrn())
                .clientEventId(event.clientEventId())
                .localStoragePlayback(event.isOfflineTrack())
                .consumerSubsPlan(featureOperations.getCurrentPlan())
                .trigger(getTrigger(event.trackSourceInfo()))
                .protocol(event.protocol())
                .playerType(event.playerType())
                .monetizationModel(event.monetizationModel());
        if (event.adUrn().isPresent()) {
            data.adUrn(event.adUrn().get());
        }
        if (event.policy().isPresent()) {
            data.policy(event.policy().get());
        }
        if (event.monetizationType().isPresent()) {
            data.monetizationType(event.monetizationType().get());
        }
        if (event.promoterUrn().isPresent()) {
            data.promotedBy(event.promoterUrn().get().toString());
        }
        if (event.stopReason().isPresent()) {
            data.reason(event.stopReason().get().key());
        }
        if (event.playId().isPresent()) {
            data.playId(event.playId().get());
        }
        addTrackSourceInfoToSessionEvent(data, event.trackSourceInfo(), urn);

        return data;
    }

    private String transform(EventLoggerEventData data) {
        try {
            return jsonTransformer.toJson(data);
        } catch (ApiMapperException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String mapToJson(HashMap<String, Object> data) {
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
        EventLoggerEventDataV1 eventData = new EventLoggerEventDataV1(eventName,
                                                                      BOOGALOO_VERSION,
                                                                      appId,
                                                                      getAnonymousId(),
                                                                      getUserUrn(),
                                                                      timestamp,
                                                                      connectionHelper.getCurrentConnectionType()
                                                                                      .getValue(),
                                                                      String.valueOf(deviceHelper.getAppVersionCode()));
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

    private String getTrigger(TrackSourceInfo trackSourceInfo) {
        return trackSourceInfo.getIsUserTriggered() ? "manual" : "auto";
    }

    private EventLoggerEventData addTrackSourceInfoToSessionEvent(EventLoggerEventData data,
                                                                  TrackSourceInfo sourceInfo,
                                                                  Urn urn) {
        if (sourceInfo.hasSource()) {
            data.source(sourceInfo.getSource());
            data.sourceVersion(sourceInfo.getSourceVersion());
        }
        if (sourceInfo.isFromPlaylist() && !sourceInfo.getCollectionUrn().isLocal()) {
            data.inPlaylist(sourceInfo.getCollectionUrn());
            data.playlistPosition(sourceInfo.getPlaylistPosition());
        }

        if (sourceInfo.hasReposter()) {
            data.reposter(sourceInfo.getReposter());
        }

        if (sourceInfo.isFromSearchQuery()) {
            SearchQuerySourceInfo searchQuerySourceInfo = sourceInfo.getSearchQuerySourceInfo();
            data.queryUrn(searchQuerySourceInfo.getQueryUrn().toString());
            data.queryPosition(searchQuerySourceInfo.getUpdatedResultPosition(urn));
        }

        if (sourceInfo.isFromStation()) {
            // When updating it, please update V0 too. Your friend.
            data.sourceUrn(sourceInfo.getCollectionUrn().toString());

            if (!sourceInfo.getStationsSourceInfo().getQueryUrn().equals(Urn.NOT_SET)) {
                data.queryUrn(sourceInfo.getStationsSourceInfo().getQueryUrn().toString());
            }
        }

        if (sourceInfo.hasQuerySourceInfo()) {
            QuerySourceInfo querySourceInfo = sourceInfo.getQuerySourceInfo();
            data.queryUrn(querySourceInfo.getQueryUrn().toString());
            data.queryPosition(querySourceInfo.getQueryPosition());
        }

        return data;
    }
}
