package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.ACTION_NAVIGATION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.AUDIO_ACTION_CHECKPOINT;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.AUDIO_ACTION_PAUSE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.AUDIO_ACTION_PLAY;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.AUDIO_ACTION_PLAY_START;
import static com.soundcloud.android.events.AdPlaybackSessionEvent.EVENT_KIND_CHECKPOINT;
import static com.soundcloud.android.events.AdPlaybackSessionEvent.EVENT_KIND_PLAY;
import static com.soundcloud.android.events.AdPlaybackSessionEvent.EVENT_KIND_STOP;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.discovery.ChartSourceInfo;
import com.soundcloud.android.discovery.RecommendationsSourceInfo;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdPlaybackErrorEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.android.events.PlayableTrackingKeys;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;

public class EventLoggerV1JsonDataBuilder {

    private static final String AUDIO_EVENT = "audio";
    private static final String CLICK_EVENT = "click";
    private static final String OFFLINE_SYNC_EVENT = "offline_sync";
    private static final String IMPRESSION_EVENT = "impression";
    private static final String INTERACTION_EVENT = "item_interaction";

    // Ads specific events
    private static final String RICH_MEDIA_ERROR_EVENT = "rich_media_stream_error";
    private static final String RICH_MEDIA_STREAM_EVENT = "rich_media_stream";
    private static final String RICH_MEDIA_PERFORMANCE_EVENT = "rich_media_stream_performance";
    private static final String EXPERIMENT_VARIANTS_KEY = "part_of_variants";

    private static final String BOOGALOO_VERSION = "v1.21.2";

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

    public String buildForAdDelivery(AdDeliveryEvent event) {
        switch (event.getKind()) {
            case AdDeliveryEvent.AD_DELIVERED_KIND:
                return buildAdDeliveredEvent(event);
            case AdDeliveryEvent.AD_FAILED_KIND:
                return buildAdRequestFailedEvent(event);
            default:
                throw new IllegalStateException("Unexpected ad delivery type: " + event);
        }
    }

    private EventLoggerEventData buildBaseAdDeliveryEvent(AdDeliveryEvent eventData) {
        return buildBaseEvent("ad_delivery", eventData)
                .monetizedObject(eventData.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .playerVisible(eventData.playerVisible)
                .inForeground(eventData.inForeground)
                .adsRequested(eventData.adsRequested)
                .adsEndpoint(eventData.get(PlayableTrackingKeys.KEY_ADS_ENDPOINT));
    }

    private String buildAdDeliveredEvent(AdDeliveryEvent eventData) {
        final EventLoggerEventData data = buildBaseAdDeliveryEvent(eventData)
                .adsRequestSuccess(true)
                .adOptimized(eventData.adOptimized)
                .adsReceived(mapToJson(eventData.adsReceived.ads));

        if (eventData.adUrn.isAd()) {
            data.adUrn(eventData.adUrn.toString());
        }

        return transform(data);
    }

    private String buildAdRequestFailedEvent(AdDeliveryEvent eventData) {
        return transform(buildBaseAdDeliveryEvent(eventData)
                                 .adsRequestSuccess(false));
    }

    public String buildForAdProgressQuartileEvent(AdPlaybackSessionEvent eventData) {
        return transform(buildBaseEvent(CLICK_EVENT, eventData)
                                 .clickName(eventData.get(PlayableTrackingKeys.KEY_QUARTILE_TYPE))
                                 .adUrn(eventData.get(PlayableTrackingKeys.KEY_AD_URN))
                                 .pageName(eventData.trackSourceInfo.getOriginScreen())
                                 .monetizedObject(eventData.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                                 .monetizationType(eventData.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)));
    }

    public String buildForAdFinished(AdPlaybackSessionEvent eventData) {
        return transform(buildBaseEvent(CLICK_EVENT, eventData)
                                 .clickName("ad::finish")
                                 .adUrn(eventData.get(PlayableTrackingKeys.KEY_AD_URN))
                                 .pageName(eventData.trackSourceInfo.getOriginScreen())
                                 .monetizedObject(eventData.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                                 .monetizationType(eventData.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)));
    }

    public String buildForAdImpression(AdPlaybackSessionEvent eventData) {
        return transform(buildBaseEvent(IMPRESSION_EVENT, eventData)
                                 .adUrn(eventData.get(PlayableTrackingKeys.KEY_AD_URN))
                                 .pageName(eventData.trackSourceInfo.getOriginScreen())
                                 .impressionName(eventData.isVideoAd() ? "video_ad_impression" : "audio_ad_impression")
                                 .monetizedObject(eventData.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                                 .monetizationType(eventData.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)));
    }

    public String buildForRichMediaSessionEvent(AdPlaybackSessionEvent eventData) {
        EventLoggerEventData data = buildBaseEvent(RICH_MEDIA_STREAM_EVENT, eventData)
                .adUrn(eventData.get(PlayableTrackingKeys.KEY_AD_URN))
                .monetizedObject(eventData.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(eventData.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE))
                .pageName(eventData.trackSourceInfo.getOriginScreen())
                .playheadPosition(eventData.getEventArgs().getProgress())
                .clientEventId(eventData.getEventArgs().getUuid())
                .trigger(getTrigger(eventData.trackSourceInfo))
                .protocol(eventData.getEventArgs().getProtocol())
                .playerType(eventData.getEventArgs().getPlayerType())
                .trackLength(eventData.getEventArgs().getDuration());

        switch (eventData.getKind()) {
            case EVENT_KIND_PLAY:
                data.action(AUDIO_ACTION_PLAY);
                break;
            case EVENT_KIND_STOP:
                data.action(AUDIO_ACTION_PAUSE);
                data.reason(getStopReason(eventData.getStopReason()));
                break;
            case EVENT_KIND_CHECKPOINT:
                data.action(AUDIO_ACTION_CHECKPOINT);
                break;
            default:
                throw new IllegalArgumentException("Unexpected audio event:" + eventData.getKind());
        }

        addTrackSourceInfoToSessionEvent(data, eventData.trackSourceInfo, Urn.NOT_SET);

        return transform(data);
    }

    public EventLoggerEventData buildAdClickThroughEvent(UIEvent event) {
        final String clickName = event.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE).equals("video_ad") ?  "clickthrough::video_ad" : "clickthrough::audio_ad";
        return buildClickEvent(clickName, event)
                .clickTarget(event.get(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL));
    }

    public String buildForRichMediaPerformance(PlaybackPerformanceEvent event) {
        return transform(buildBaseEvent(RICH_MEDIA_PERFORMANCE_EVENT, event.getTimestamp())
                                 .mediaType(event.isVideoAd() ? "video" : "audio")
                                 .protocol(event.getProtocol().getValue())
                                 .playerType(event.getPlayerType().getValue())
                                 .format(getRichMediaFormatName(event.getFormat()))
                                 .bitrate(event.getBitrate())
                                 .metric(getRichMediaPerformanceEventType(event.getMetric()), event.getMetricValue())
                                 .host(event.getCdnHost()));
    }

    public String buildForRichMediaErrorEvent(AdPlaybackErrorEvent eventData) {
        return transform(buildBaseEvent(RICH_MEDIA_ERROR_EVENT, eventData)
                                 .mediaType(eventData.getMediaType())
                                 .protocol(eventData.getProtocol())
                                 .playerType(eventData.getPlayerType())
                                 .format(getRichMediaFormatName(eventData.getFormat()))
                                 .bitrate(eventData.getBitrate())
                                 .errorName(eventData.getKind())
                                 .host(eventData.getHost()));
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

    public String buildForUpsell(UpgradeFunnelEvent event) {
        switch (event.getKind()) {
            case UpgradeFunnelEvent.KIND_UPSELL_CLICK:
                return transform(buildBaseEvent(CLICK_EVENT, event)
                                         .clickCategory(EventLoggerClickCategories.CONSUMER_SUBS)
                                         .clickName("clickthrough::consumer_sub_ad")
                                         .clickObject(getUpsellTrackingCode(event))
                                         .pageName(event.get(UpgradeFunnelEvent.KEY_PAGE_NAME))
                                         .pageUrn(event.get(UpgradeFunnelEvent.KEY_PAGE_URN)));

            case UpgradeFunnelEvent.KIND_UPSELL_IMPRESSION:
                return transform(buildBaseEvent(IMPRESSION_EVENT, event)
                                         .impressionName("consumer_sub_ad")
                                         .impressionObject(getUpsellTrackingCode(event))
                                         .pageName(event.get(UpgradeFunnelEvent.KEY_PAGE_NAME))
                                         .pageUrn(event.get(UpgradeFunnelEvent.KEY_PAGE_URN)));

            case UpgradeFunnelEvent.KIND_UPGRADE_SUCCESS:
                return transform(buildBaseEvent(IMPRESSION_EVENT, event)
                                         .impressionName("consumer_sub_upgrade_success"));

            case UpgradeFunnelEvent.KIND_RESUBSCRIBE_CLICK:
                return transform(buildBaseEvent(CLICK_EVENT, event)
                                         .clickCategory(EventLoggerClickCategories.CONSUMER_SUBS)
                                         .clickName("clickthrough::consumer_sub_resubscribe")
                                         .clickObject(getUpsellTrackingCode(event))
                                         .pageName(event.get(UpgradeFunnelEvent.KEY_PAGE_NAME)));

            case UpgradeFunnelEvent.KIND_RESUBSCRIBE_IMPRESSION:
                return transform(buildBaseEvent(IMPRESSION_EVENT, event)
                                         .impressionName("consumer_sub_resubscribe")
                                         .impressionObject(getUpsellTrackingCode(event))
                                         .pageName(event.get(UpgradeFunnelEvent.KEY_PAGE_NAME)));

            default:
                throw new IllegalArgumentException("Unexpected upsell tracking event type " + event);
        }
    }

    private String getUpsellTrackingCode(UpgradeFunnelEvent event) {
        return TrackingCode.fromEventId(event.get(UpgradeFunnelEvent.KEY_ID));
    }

    public String buildForUIEvent(UIEvent event) {
        switch (event.getKind()) {
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
            case UIEvent.KIND_SHUFFLE:
                return transform(buildPlaybackClickEvent("shuffle:on", event));
            case UIEvent.KIND_VIDEO_AD_FULLSCREEN:
                return transform(buildClickEvent("ad::full_screen", event));
            case UIEvent.KIND_VIDEO_AD_SHRINK:
                return transform(buildClickEvent("ad::exit_full_screen", event));
            case UIEvent.KIND_AD_CLICKTHROUGH:
                return transform(buildAdClickThroughEvent(event));
            case UIEvent.KIND_SKIP_AD_CLICK:
                return transform(buildClickEvent("ad::skip", event));
            case UIEvent.KIND_NAVIGATION:
                return transform(buildInteractionEvent(ACTION_NAVIGATION, event));
            default:
                throw new IllegalStateException("Unexpected UIEvent type: " + event);
        }
    }

    public String buildForInteractionEvent(UIEvent event) {
        switch (event.getKind()) {
            case UIEvent.KIND_SHARE:
                return transform(buildInteractionEvent("share", event));
            case UIEvent.KIND_REPOST:
                return transform(buildInteractionEvent("repost::add", event));
            case UIEvent.KIND_UNREPOST:
                return transform(buildInteractionEvent("repost::remove", event));
            case UIEvent.KIND_LIKE:
                return transform(buildInteractionEvent("like::add", event));
            case UIEvent.KIND_UNLIKE:
                return transform(buildInteractionEvent("like::remove", event));
            default:
                throw new IllegalStateException("Unexpected UIEvent type: " + event);
        }
    }

    public String buildForOfflineInteractionEvent(OfflineInteractionEvent event) {
        if (OfflineInteractionEvent.KIND_LIMIT_BELOW_USAGE.equals(event.getKind())) {
            return transform(buildBaseEvent(IMPRESSION_EVENT, event)
                                     .impressionCategory("consumer_subs")
                                     .impressionName(event.getKind())
                                     .pageName(event.getPageName()));
        } else {
            return transform(buildBaseEvent(CLICK_EVENT, event)
                                     .clickCategory(EventLoggerClickCategories.CONSUMER_SUBS)
                                     .clickName(event.getKind())
                                     .pageName(event.getPageName())
                                     .clickObject(event.getClickObject())
                                     .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                                     .monetizationType(event.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE))
                                     .promotedBy(event.get(PlayableTrackingKeys.KEY_PROMOTER_URN)));
        }
    }

    public String buildForOfflinePerformanceEvent(OfflinePerformanceEvent event) {
        final EventLoggerEventData eventLoggerEventData = buildBaseEvent(OFFLINE_SYNC_EVENT, event)
                .eventStage(event.getKind())
                .track(event.getTrackUrn())
                .trackOwner(event.getTrackOwner())
                .inOfflineLikes(event.isFromLikes())
                .inOfflinePlaylist(event.partOfPlaylist());
        return transform(eventLoggerEventData);
    }

    public String buildForCollectionEvent(CollectionEvent event) {
        switch (event.getKind()) {
            case CollectionEvent.KIND_SET:
                return transform(buildCollectionEvent("filter_sort::set", event));
            case CollectionEvent.KIND_CLEAR:
                return transform(buildCollectionEvent("filter_sort::clear", event));
            case CollectionEvent.KIND_RECENTLY_PLAYED_NAVIGATION:
                return transform(buildNavigationCollectionEvent(event));
            default:
                throw new IllegalStateException("Unexpected CollectionEvent type: " + event);
        }
    }

    private EventLoggerEventData buildPlaybackClickEvent(String clickName, UIEvent event) {
        EventLoggerEventData eventData =
                buildClickEvent(clickName, event).clickCategory(EventLoggerClickCategories.PLAYBACK);

        final String pageUrn = event.get(PlayableTrackingKeys.KEY_PAGE_URN);

        if (pageUrn != null && !pageUrn.equals(Urn.NOT_SET.toString())) {
            eventData.pageUrn(pageUrn);
        }
        return eventData;
    }

    private EventLoggerEventData buildCollectionEvent(String clickName, CollectionEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .clickName(clickName)
                .pageName(Screen.COLLECTIONS.get())
                .clickCategory(EventLoggerClickCategories.COLLECTION)
                .clickObject(event.get(CollectionEvent.KEY_OBJECT))
                .clickTarget(event.get(CollectionEvent.KEY_TARGET));
    }

    private EventLoggerEventData buildNavigationCollectionEvent(CollectionEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .clickName(CollectionEvent.CLICK_NAME_ITEM_NAVIGATION)
                .pageName(event.get(CollectionEvent.KEY_PAGE_NAME))
                .clickObject(event.get(CollectionEvent.KEY_OBJECT));
    }

    private EventLoggerEventData buildClickEvent(String clickName, UIEvent event) {
        return buildBaseEvent(CLICK_EVENT, event)
                .clickName(clickName)
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .monetizationType(event.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE))
                .monetizedObject(event.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .promotedBy(event.get(PlayableTrackingKeys.KEY_PROMOTER_URN))
                .clickObject(event.get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN));
    }

    private EventLoggerEventData buildEngagementEvent(String engagementClickName, UIEvent event) {
        EventLoggerEventData eventData = buildClickEvent(engagementClickName, event)
                .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                .clickSource(event.getClickSource());

        final Optional<Urn> sourceUrn = event.getClickSourceUrn();
        final Optional<Urn> queryUrn = event.getQueryUrn();
        final Optional<Integer> queryPosition = event.getQueryPosition();

        if (sourceUrn.isPresent() && !sourceUrn.get().equals(Urn.NOT_SET)) {
            eventData.clickSourceUrn(sourceUrn.get().toString());
        }

        if (queryUrn.isPresent() && !queryUrn.get().equals(Urn.NOT_SET)) {
            eventData.queryUrn(queryUrn.get().toString());
        }

        if (queryPosition.isPresent()) {
            eventData.queryPosition(queryPosition.get());
        }

        final String pageUrn = event.get(PlayableTrackingKeys.KEY_PAGE_URN);
        if (pageUrn != null && !pageUrn.equals(Urn.NOT_SET.toString())) {
            eventData.pageUrn(pageUrn);
        }

        if (event.isFromOverflow()) {
            eventData.fromOverflowMenu(event.isFromOverflow());
        }

        return eventData;
    }

    private EventLoggerEventData buildInteractionEvent(String action, UIEvent event) {
        final Optional<AttributingActivity> attributingActivity = event.getAttributingActivity();
        final Optional<Module> module = event.getModule();
        final String pageUrn = event.get(PlayableTrackingKeys.KEY_PAGE_URN);

        final EventLoggerEventData eventData = buildBaseEvent(INTERACTION_EVENT, event)
                .clientEventId(event.getId())
                .item(event.get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN))
                .pageviewId(event.get(ReferringEvent.REFERRING_EVENT_ID_KEY))
                .pageName(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN))
                .action(action);

        if (action.equals(ACTION_NAVIGATION)) {
            eventData.linkType(event.getLinkType());
        }

        if (pageUrn != null && !pageUrn.equals(Urn.NOT_SET.toString())) {
            eventData.pageUrn(pageUrn);
        }

        if (attributingActivity.isPresent() && attributingActivity.get().isActive(module)) {
            eventData.attributingActivity(attributingActivity.get().getType(), attributingActivity.get().getResource());
        }

        if (module.isPresent()) {
            eventData.module(module.get().getName());
            eventData.modulePosition(module.get().getPosition());
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
                .clientEventId(event.getClientEventId())
                .localStoragePlayback(event.isOfflineTrack())
                .consumerSubsPlan(featureOperations.getCurrentPlan())
                .trigger(getTrigger(event.getTrackSourceInfo()))
                .protocol(event.get(PlaybackSessionEvent.KEY_PROTOCOL))
                .playerType(event.get(PlaybackSessionEvent.PLAYER_TYPE))
                .adUrn(event.get(PlayableTrackingKeys.KEY_AD_URN))
                .policy(event.get(PlaybackSessionEvent.KEY_POLICY))
                .monetizationModel(event.getMonetizationModel())
                .monetizedObject(event.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN))
                .monetizationType(event.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE))
                .promotedBy(event.get(PlayableTrackingKeys.KEY_PROMOTER_URN));

        if (event.isPlayEvent()) {
            data.action(AUDIO_ACTION_PLAY);
            data.playId(event.getPlayId());
        } else if (event.isPlayStartEvent()) {
            data.action(AUDIO_ACTION_PLAY_START);
        } else if (event.isStopEvent()) {
            data.action(AUDIO_ACTION_PAUSE);
            data.reason(getStopReason(event.getStopReason()));
            data.playId(event.getPlayId());
        } else if (event.isCheckpointEvent()) {
            data.action(AUDIO_ACTION_CHECKPOINT);
            data.playId(event.getPlayId());
        } else {
            throw new IllegalArgumentException("Unexpected audio event:" + event.getKind());
        }

        addTrackSourceInfoToSessionEvent(data, event.getTrackSourceInfo(), urn);

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

    public EventLoggerEventData addTrackSourceInfoToSessionEvent(EventLoggerEventData data,
                                                                 TrackSourceInfo sourceInfo,
                                                                 Urn urn) {
        if (sourceInfo.hasSource()) {
            data.source(sourceInfo.getSource());
            data.sourceVersion(sourceInfo.getSourceVersion());
        }
        if (sourceInfo.isFromPlaylist()) {
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

        if (sourceInfo.isFromRecommendations()) {
            RecommendationsSourceInfo recommendationsSourceInfo = sourceInfo.getRecommendationsSourceInfo();
            data.queryUrn(recommendationsSourceInfo.getQueryUrn().toString());
            data.queryPosition(recommendationsSourceInfo.getQueryPosition());
        }

        if (sourceInfo.isFromChart()) {
            final ChartSourceInfo chartSourceInfo = sourceInfo.getChartSourceInfo();
            data.queryPosition(chartSourceInfo.getQueryPosition());
            data.queryUrn(chartSourceInfo.getQueryUrn().toString());
        }

        return data;
    }

    private String getStopReason(int stopReason) {
        switch (stopReason) {
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
                throw new IllegalArgumentException("Unexpected stop reason : " + stopReason);
        }
    }
}