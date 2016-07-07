package com.soundcloud.android.events;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
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
    public static final String KIND_NAVIGATION = "navigation";
    public static final String KIND_PLAYER_OPEN = "player_open";
    public static final String KIND_PLAYER_CLOSE = "player_close";
    public static final String KIND_VIDEO_AD_CLICKTHROUGH = "video_ad_click_through";
    public static final String KIND_VIDEO_AD_FULLSCREEN = "video_ad_fullscreen";
    public static final String KIND_VIDEO_AD_SHRINK = "video_ad_shrink";
    public static final String KIND_AUDIO_AD_CLICK = "audio_ad_click";
    public static final String KIND_SKIP_AUDIO_AD_CLICK = "skip_audio_ad_click";
    public static final String KIND_SKIP_VIDEO_AD_CLICK = "skip_video_ad_click";

    public static UIEvent fromPlayerOpen() {
        return new UIEvent(KIND_PLAYER_OPEN);
    }

    public static UIEvent fromPlayerClose() {
        return new UIEvent(KIND_PLAYER_CLOSE);
    }

    public static UIEvent fromToggleFollow(boolean isFollow, @NonNull EntityMetadata userMetadata) {
        return new UIEvent(isFollow ? KIND_FOLLOW : KIND_UNFOLLOW).putPlayableMetadata(userMetadata);
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

    public static UIEvent fromProfileNav() {
        return new UIEvent(KIND_NAVIGATION).put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, "you");
    }

    public static UIEvent fromStreamNav() {
        return new UIEvent(KIND_NAVIGATION).put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, "stream");
    }

    public static UIEvent fromExploreNav() {
        return new UIEvent(KIND_NAVIGATION).put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, "explore");
    }

    public static UIEvent fromLikesNav() {
        return new UIEvent(KIND_NAVIGATION).put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, "collection_likes");
    }

    public static UIEvent fromPlaylistsNav() {
        return new UIEvent(KIND_NAVIGATION).put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, "collection_playlists");
    }

    public static UIEvent fromSearchAction() {
        return new UIEvent(KIND_NAVIGATION).put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, "search");
    }

    public static UIEvent fromVideoAdFullscreen(VideoAd videoAd, @Nullable TrackSourceInfo trackSourceInfo) {
        final UIEvent event = new UIEvent(KIND_VIDEO_AD_FULLSCREEN, System.currentTimeMillis());
        return withBasicVideoAdAttributes(event, videoAd, trackSourceInfo)
                .addPromotedTrackingUrls(SIZE_CHANGES, videoAd.getFullScreenUrls());
    }

    public static UIEvent fromVideoAdShrink(VideoAd videoAd, @Nullable TrackSourceInfo trackSourceInfo) {
        final UIEvent event = new UIEvent(KIND_VIDEO_AD_SHRINK, System.currentTimeMillis());
        return withBasicVideoAdAttributes(event, videoAd, trackSourceInfo)
                .addPromotedTrackingUrls(SIZE_CHANGES, videoAd.getExitFullScreenUrls());
    }

    public static UIEvent fromSkipVideoAdClick(VideoAd videoAd, @Nullable TrackSourceInfo trackSourceInfo) {
        final UIEvent event = new UIEvent(KIND_SKIP_VIDEO_AD_CLICK, System.currentTimeMillis());
        return withBasicVideoAdAttributes(event, videoAd, trackSourceInfo)
                .addPromotedTrackingUrls(SKIPS, videoAd.getSkipUrls());
    }

    public static UIEvent fromVideoAdClickThrough(VideoAd videoAd, TrackSourceInfo trackSourceInfo) {
        final UIEvent event = new UIEvent(KIND_VIDEO_AD_CLICKTHROUGH, System.currentTimeMillis());
        return withBasicVideoAdAttributes(event, videoAd, trackSourceInfo)
                .addPromotedTrackingUrls(CLICKTHROUGHS, videoAd.getClickUrls())
                .put(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL, videoAd.getClickThroughUrl());
    }

    private static UIEvent withBasicVideoAdAttributes(UIEvent adEvent,
                                                      VideoAd videoAd,
                                                      TrackSourceInfo trackSourceInfo) {
        return adEvent
                .put(PlayableTrackingKeys.KEY_AD_URN, videoAd.getAdUrn().toString())
                .put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, videoAd.getMonetizableTrackUrn().toString())
                .put(PlayableTrackingKeys.KEY_MONETIZATION_TYPE, TYPE_VIDEO_AD)
                .put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, getNotNullOriginScreen(trackSourceInfo));
    }

    public static UIEvent fromAudioAdClick(AudioAd audioAd,
                                           Urn audioAdTrack,
                                           Urn user,
                                           @Nullable TrackSourceInfo trackSourceInfo) {
        return fromAudioAdCompanionDisplayClick(audioAd,
                                                audioAdTrack,
                                                user,
                                                trackSourceInfo,
                                                System.currentTimeMillis());
    }

    public static UIEvent fromSkipAudioAdClick(AudioAd audioAd,
                                               Urn audioAdTrack,
                                               Urn user,
                                               @Nullable TrackSourceInfo trackSourceInfo) {
        return fromSkipAudioAdClick(audioAd, audioAdTrack, user, trackSourceInfo, System.currentTimeMillis());
    }

    @VisibleForTesting
    public static UIEvent fromAudioAdCompanionDisplayClick(AudioAd audioAd,
                                                           Urn audioAdTrack,
                                                           Urn user,
                                                           @Nullable TrackSourceInfo trackSourceInfo,
                                                           long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(KIND_AUDIO_AD_CLICK, timestamp),
                                          audioAd,
                                          audioAdTrack,
                                          user,
                                          trackSourceInfo)
                .addPromotedTrackingUrls(CLICKTHROUGHS, audioAd.getCompanionClickUrls())
                .put(PlayableTrackingKeys.KEY_AD_URN, audioAd.getCompanionAdUrn())
                .put(PlayableTrackingKeys.KEY_AD_ARTWORK_URL, audioAd.getCompanionImageUrl())
                .put(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL, audioAd.getClickThroughUrl());
    }

    @VisibleForTesting
    public static UIEvent fromSkipAudioAdClick(AudioAd audioAd,
                                               Urn audioAdTrack,
                                               Urn user,
                                               @Nullable TrackSourceInfo trackSourceInfo,
                                               long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(KIND_SKIP_AUDIO_AD_CLICK, timestamp),
                                          audioAd,
                                          audioAdTrack,
                                          user,
                                          trackSourceInfo)
                .<UIEvent>put(PlayableTrackingKeys.KEY_AD_URN, audioAd.getAdUrn().toString())
                .addPromotedTrackingUrls(SKIPS, audioAd.getSkipUrls());
    }

    public static UIEvent fromCreatePlaylist(EntityMetadata metadata) {
        return new UIEvent(KIND_CREATE_PLAYLIST)
                .putPlayableMetadata(metadata);
    }

    private static UIEvent withBasicAudioAdAttributes(UIEvent event,
                                                      AudioAd audioAd,
                                                      Urn audioAdTrack,
                                                      Urn user,
                                                      @Nullable TrackSourceInfo trackSourceInfo) {
        return event
                .put(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN, audioAdTrack.toString())
                .put(PlayableTrackingKeys.KEY_USER_URN, user.toString())
                .put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, audioAd.getMonetizableTrackUrn().toString())
                .put(PlayableTrackingKeys.KEY_AD_TRACK_URN, audioAdTrack.toString())
                .put(PlayableTrackingKeys.KEY_AD_ARTWORK_URL, audioAd.getCompanionImageUrl())
                .put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, getNotNullOriginScreen(trackSourceInfo));
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
