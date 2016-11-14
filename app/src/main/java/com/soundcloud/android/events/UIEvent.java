package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UIEvent extends TrackingEvent {

    public static final String TYPE_MONETIZABLE_PROMOTED = "promoted";
    private static final String CLICKTHROUGHS = "CLICKTHROUGHS";
    private static final String SKIPS = "SKIPS";
    private static final String SIZE_CHANGES = "SIZE_CHANGES";
    private static final String TYPE_VIDEO_AD = "video_ad";
    private static final String TYPE_AUDIO_AD = "audio_ad";
    private static final String TYPE_MOBILE_INLAY = "mobile_inlay";
    private static final String TRIGGER_AUTO = "auto";
    private static final String TRIGGER_MANUAL = "manual";
    private static final String SHUFFLE_ON = "shuffle::on";
    private static final String SHUFFLE_OFF = "shuffle::off";
    private final Map<String, List<String>> promotedTrackingUrls;

    private EventContextMetadata eventContextMetadata;
    public static final String KIND_FOLLOW = "follow";

    public static final String KIND_UNFOLLOW = "unfollow";
    public static final String KIND_LIKE = "like";
    public static final String KIND_UNLIKE = "unlike";
    public static final String KIND_REPOST = "repost";
    public static final String KIND_UNREPOST = "unrepost";
    public static final String KIND_ADD_TO_PLAYLIST = "add_to_playlist";
    public static final String KIND_CREATE_PLAYLIST = "create_playlist";
    public static final String KIND_COMMENT = "comment";
    public static final String KIND_SHARE = "share";
    public static final String KIND_SHUFFLE = "shuffle";
    public static final String KIND_PLAY_QUEUE_SHUFFLE = "play_queue_shuffle";
    public static final String KIND_SWIPE_SKIP = "swipe_skip";
    public static final String KIND_SYSTEM_SKIP = "system_skip";
    public static final String KIND_BUTTON_SKIP = "button_skip";
    public static final String KIND_NAVIGATION = "navigation";
    public static final String KIND_PLAYER_OPEN = "player_open";
    public static final String KIND_PLAYER_CLOSE = "player_close";
    public static final String KIND_VIDEO_AD_FULLSCREEN = "video_ad_fullscreen";
    public static final String KIND_VIDEO_AD_SHRINK = "video_ad_shrink";
    public static final String KIND_AD_CLICKTHROUGH = "ad_click_through";
    public static final String KIND_SKIP_AD_CLICK = "skip_ad_click";
    public static final String KIND_START_STATION = "start_station";
    public static final String KEY_CLICK_NAME = "click_name";
    public static final String KIND_PLAY_QUEUE_OPEN = "play_queue_open";
    public static final String KIND_PLAY_QUEUE_CLOSE = "play_queue_close";

    public static UIEvent fromPlayerOpen(boolean manual) {
        return new UIEvent(KIND_PLAYER_OPEN)
                .put(PlayableTrackingKeys.KEY_TRIGGER, manual ? TRIGGER_MANUAL : TRIGGER_AUTO);
    }

    public static UIEvent fromPlayerClose(boolean manual) {
        return new UIEvent(KIND_PLAYER_CLOSE)
                .put(PlayableTrackingKeys.KEY_TRIGGER, manual ? TRIGGER_MANUAL : TRIGGER_AUTO);
    }

    public static UIEvent fromToggleFollow(boolean isFollow,
                                           @NonNull EntityMetadata userMetadata,
                                           EventContextMetadata eventContextMetadata) {
        return new UIEvent(isFollow ? KIND_FOLLOW : KIND_UNFOLLOW)
                .putPlayableMetadata(userMetadata)
                .putEventContextMetadata(eventContextMetadata);
    }

    public static UIEvent fromToggleLike(boolean isLike,
                                         @NonNull Urn resourceUrn,
                                         @NonNull EventContextMetadata contextMetadata,
                                         @Nullable PromotedSourceInfo promotedSourceInfo,
                                         @NonNull EntityMetadata playable) {
        return new UIEvent(isLike ? KIND_LIKE : KIND_UNLIKE)
                .<UIEvent>put(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .putEventContextMetadata(contextMetadata)
                .putPromotedItemKeys(promotedSourceInfo)
                .putPlayableMetadata(playable);
    }

    public static UIEvent fromToggleRepost(boolean isRepost,
                                           @NonNull Urn resourceUrn,
                                           @NonNull EventContextMetadata contextMetadata,
                                           @Nullable PromotedSourceInfo promotedSourceInfo,
                                           @NonNull EntityMetadata entityMetadata) {
        return new UIEvent(isRepost ? KIND_REPOST : KIND_UNREPOST)
                .<UIEvent>put(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .putEventContextMetadata(contextMetadata)
                .putPromotedItemKeys(promotedSourceInfo)
                .putPlayableMetadata(entityMetadata);
    }

    public static UIEvent fromAddToPlaylist(EventContextMetadata eventContextMetadata) {
        return new UIEvent(KIND_ADD_TO_PLAYLIST)
                .putEventContextMetadata(eventContextMetadata);
    }

    public static UIEvent fromComment(EventContextMetadata eventContextMetadata,
                                      @NonNull EntityMetadata playable) {
        return new UIEvent(KIND_COMMENT)
                .putEventContextMetadata(eventContextMetadata)
                .putPlayableMetadata(playable);
    }

    public static UIEvent fromShare(@NonNull Urn resourceUrn,
                                    @NonNull EventContextMetadata contextMetadata,
                                    @Nullable PromotedSourceInfo promotedSourceInfo,
                                    @NonNull EntityMetadata playable) {
        return new UIEvent(KIND_SHARE)
                .<UIEvent>put(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .putEventContextMetadata(contextMetadata)
                .putPromotedItemKeys(promotedSourceInfo)
                .putPlayableMetadata(playable);
    }

    public static UIEvent fromShuffle(@NotNull EventContextMetadata contextMetadata) {
        return new UIEvent(KIND_SHUFFLE).putEventContextMetadata(contextMetadata);
    }

    public static UIEvent fromSystemSkip() {
        return new UIEvent(KIND_SYSTEM_SKIP);
    }

    public static UIEvent fromButtonSkip() {
        return new UIEvent(KIND_BUTTON_SKIP);
    }

    public static UIEvent fromSwipeSkip() {
        return new UIEvent(KIND_SWIPE_SKIP);
    }

    public static UIEvent fromVideoAdFullscreen(VideoAd videoAd, @Nullable TrackSourceInfo trackSourceInfo) {
        final UIEvent event = new UIEvent(KIND_VIDEO_AD_FULLSCREEN, System.currentTimeMillis());
        return withPlayerAdAttributes(event, videoAd, trackSourceInfo)
                .addPromotedTrackingUrls(SIZE_CHANGES, videoAd.getFullScreenUrls());
    }

    public static UIEvent fromVideoAdShrink(VideoAd videoAd, @Nullable TrackSourceInfo trackSourceInfo) {
        final UIEvent event = new UIEvent(KIND_VIDEO_AD_SHRINK, System.currentTimeMillis());
        return withPlayerAdAttributes(event, videoAd, trackSourceInfo)
                .addPromotedTrackingUrls(SIZE_CHANGES, videoAd.getExitFullScreenUrls());
    }

    public static UIEvent fromSkipAdClick(PlayerAdData adData, @Nullable TrackSourceInfo trackSourceInfo) {
        final UIEvent event = new UIEvent(KIND_SKIP_AD_CLICK, System.currentTimeMillis());
        return withPlayerAdAttributes(event, adData, trackSourceInfo)
                .addPromotedTrackingUrls(SKIPS, adData.getSkipUrls());
    }

    public static UIEvent fromPlayerAdClickThrough(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        final UIEvent event = withPlayerAdAttributes(new UIEvent(KIND_AD_CLICKTHROUGH), adData, trackSourceInfo)
                .addPromotedTrackingUrls(CLICKTHROUGHS, adData.getClickUrls())
                .put(PlayableTrackingKeys.KEY_CLICK_THOUGH_KIND, "clickthrough::" + getMonetizationType(adData));

        if (adData instanceof AudioAd) {
            final AudioAd audioAd = (AudioAd) adData;
            event.put(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL, audioAd.getClickThroughUrl());
            event.put(PlayableTrackingKeys.KEY_AD_ARTWORK_URL, audioAd.getCompanionImageUrl());
        } else {
            event.put(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL, ((VideoAd) adData).getClickThroughUrl());
        }

        return event;
    }

    public static UIEvent fromAppInstallAdClickThrough(AppInstallAd adData) {
        return withBasicAdAttributes(new UIEvent(KIND_AD_CLICKTHROUGH), adData)
                .addPromotedTrackingUrls(CLICKTHROUGHS, adData.getClickUrls())
                .put(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL, adData.getClickThroughUrl())
                .put(PlayableTrackingKeys.KEY_CLICK_THOUGH_KIND, "clickthrough::app_install");
    }

    public static UIEvent fromStartStation() {
        return new UIEvent(KIND_START_STATION);
    }

    public static UIEvent fromPlayQueueOpen() {
        return new UIEvent(KIND_PLAY_QUEUE_OPEN);
    }

    public static UIEvent fromPlayQueueClose() {
        return new UIEvent(KIND_PLAY_QUEUE_CLOSE);
    }

    public Optional<AttributingActivity> getAttributingActivity() {
        return Optional.fromNullable(eventContextMetadata.attributingActivity());
    }

    public Optional<Module> getModule() {
        return Optional.fromNullable(eventContextMetadata.module());
    }

    public String getLinkType() {
        final LinkType linkType = eventContextMetadata.linkType();

        return linkType == null ? null : linkType.getName();
    }

    private static UIEvent withPlayerAdAttributes(UIEvent adEvent,
                                                  PlayerAdData adData,
                                                  TrackSourceInfo trackSourceInfo)  {
        return withBasicAdAttributes(adEvent, adData)
                .put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, adData.getMonetizableTrackUrn().toString())
                .put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, getNotNullOriginScreen(trackSourceInfo));
    }

    private static UIEvent withBasicAdAttributes(UIEvent adEvent,
                                                 AdData adData) {
        return adEvent.put(PlayableTrackingKeys.KEY_AD_URN, adData.getAdUrn().toString())
                      .put(PlayableTrackingKeys.KEY_MONETIZATION_TYPE, getMonetizationType(adData));
    }

    private static String getMonetizationType(AdData adData) {
        if (adData instanceof AudioAd) {
            return TYPE_AUDIO_AD;
        } else if (adData instanceof VideoAd) {
            return TYPE_VIDEO_AD;
        } else {
            return TYPE_MOBILE_INLAY;
        }
    }

    public static UIEvent fromCreatePlaylist(EntityMetadata metadata) {
        return new UIEvent(KIND_CREATE_PLAYLIST)
                .putPlayableMetadata(metadata);
    }

    public static UIEvent fromNavigation(@NonNull Urn itemUrn,
                                         @NonNull EventContextMetadata contextMetadata) {
        return new UIEvent(KIND_NAVIGATION)
                .<UIEvent>put(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN, itemUrn.toString())
                .putEventContextMetadata(contextMetadata);
    }

    public static UIEvent fromPlayQueueShuffle(boolean isShuffled) {
        return new UIEvent(KIND_PLAY_QUEUE_SHUFFLE)
                .put(KEY_CLICK_NAME, isShuffled ? SHUFFLE_ON : SHUFFLE_OFF)
                .put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, Screen.PLAY_QUEUE.get());
    }

    private static String getNotNullOriginScreen(@Nullable TrackSourceInfo trackSourceInfo) {
        if (trackSourceInfo != null) {
            return trackSourceInfo.getOriginScreen();
        }
        return Strings.EMPTY;
    }

    @VisibleForTesting
    public UIEvent(String kind) {
        this(kind, System.currentTimeMillis());
    }

    private UIEvent(String kind, long timeStamp) {
        super(kind, timeStamp);
        promotedTrackingUrls = new HashMap<>();
    }

    private UIEvent putPromotedItemKeys(@Nullable PromotedSourceInfo promotedSourceInfo) {
        if (promotedSourceInfo != null) {
            put(PlayableTrackingKeys.KEY_AD_URN, promotedSourceInfo.getAdUrn());
            put(PlayableTrackingKeys.KEY_MONETIZATION_TYPE, TYPE_MONETIZABLE_PROMOTED);
            put(PlayableTrackingKeys.KEY_PROMOTER_URN, promotedSourceInfo.getPromoterUrn());
        }
        return this;
    }

    private UIEvent putEventContextMetadata(@NonNull EventContextMetadata contextMetadata) {
        this.eventContextMetadata = contextMetadata;

        put(PlayableTrackingKeys.KEY_PAGE_URN, contextMetadata.pageUrn().toString());
        put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, contextMetadata.pageName());
        return this;
    }

    private UIEvent putPlayableMetadata(@NonNull EntityMetadata metadata) {
        metadata.addToTrackingEvent(this);
        return this;
    }

    public String getContextScreen() {
        return eventContextMetadata.contextScreen();
    }

    public String getInvokerScreen() {
        return eventContextMetadata.invokerScreen();
    }

    public String getClickSource() {
        final TrackSourceInfo sourceInfo = eventContextMetadata.trackSourceInfo();
        if (sourceInfo != null && sourceInfo.hasSource()) {
            return sourceInfo.getSource();
        } else {
            return Strings.EMPTY;
        }
    }

    public Optional<Urn> getClickSourceUrn() {
        final TrackSourceInfo sourceInfo = eventContextMetadata.trackSourceInfo();

        if (sourceInfo != null && sourceInfo.hasCollectionUrn()) {
            return Optional.of(sourceInfo.getCollectionUrn());
        } else {
            return Optional.absent();
        }
    }

    public Optional<Urn> getQueryUrn() {
        final TrackSourceInfo sourceInfo = eventContextMetadata.trackSourceInfo();

        if (sourceInfo != null && sourceInfo.hasStationsSourceInfo()) {
            return Optional.of(sourceInfo.getStationsSourceInfo().getQueryUrn());
        } else if (sourceInfo != null && sourceInfo.isFromChart()) {
            return Optional.of(sourceInfo.getChartSourceInfo().getQueryUrn());
        } else {
            return Optional.absent();
        }
    }

    public Optional<Integer> getQueryPosition() {
        final TrackSourceInfo sourceInfo = eventContextMetadata.trackSourceInfo();

        if (sourceInfo != null && sourceInfo.isFromChart()) {
            return Optional.of(sourceInfo.getChartSourceInfo().getQueryPosition());
        } else {
            return Optional.absent();
        }
    }

    public List<String> getAdClickthroughUrls() {
        List<String> urls = promotedTrackingUrls.get(CLICKTHROUGHS);
        return urls == null ? Collections.<String>emptyList() : urls;
    }

    public List<String> getAdSkipUrls() {
        List<String> urls = promotedTrackingUrls.get(SKIPS);
        return urls == null ? Collections.<String>emptyList() : urls;
    }

    public List<String> getVideoSizeChangeUrls() {
        List<String> urls = promotedTrackingUrls.get(SIZE_CHANGES);
        return urls == null ? Collections.<String>emptyList() : urls;
    }

    public boolean isFromOverflow() {
        return eventContextMetadata.isFromOverflow();
    }

    @Override
    public String toString() {
        return String.format("UI Event with type id %s and %s", kind, attributes.toString());
    }

    private UIEvent addPromotedTrackingUrls(String key, List<String> urls) {
        promotedTrackingUrls.put(key, urls);
        return this;
    }
}
