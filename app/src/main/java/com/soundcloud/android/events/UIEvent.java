package com.soundcloud.android.events;

import com.appboy.ui.support.StringUtils;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UIEvent extends TrackingEvent {

    public static final String METHOD_TAP_FOOTER = "tap_footer";
    public static final String METHOD_HIDE_BUTTON = "hide_button";
    public static final String METHOD_BACK_BUTTON = "back_button";
    public static final String METHOD_PROFILE_OPEN = "profile_open";
    public static final String METHOD_COMMENTS_OPEN = "comments_open";
    public static final String METHOD_COMMENTS_OPEN_FROM_ADD_COMMENT = "comments_open_from_add_comment";
    public static final String METHOD_TRACK_PLAY = "track_play";
    public static final String METHOD_AD_PLAY = "ad_play";
    public static final String METHOD_SLIDE_FOOTER = "slide_footer";
    public static final String METHOD_SLIDE = "slide";

    private static final String CLICKTHROUGHS = "CLICKTHROUGHS";
    private static final String SKIPS = "SKIPS";
    private static final String TYPE_MONETIZABLE_PROMOTED = "promoted";

    private static final String TYPE_TRACK = "track";
    private static final String TYPE_PLAYLIST = "playlist";
    private static final String TYPE_UNKNOWN = "unknown";

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
    public static final String KIND_SHUFFLE_LIKES = "shuffle_likes";
    public static final String KIND_SHUFFLE_PLAYLIST = "shuffle_playlist";
    public static final String KIND_NAVIGATION = "navigation";
    public static final String KIND_AUDIO_AD_CLICK = "audio_ad_click";
    public static final String KIND_SKIP_AUDIO_AD_CLICK = "skip_audio_ad_click";
    public static final String KIND_PLAYER_OPEN = "player_open";
    public static final String KIND_PLAYER_CLOSE = "player_close";

    public static final String KIND_OFFLINE_LIKES_ADD = "offline_likes_add";
    public static final String KIND_OFFLINE_LIKES_REMOVE = "offline_likes_remove";
    public static final String KIND_OFFLINE_COLLECTION_ADD = "offline_collection_add";
    public static final String KIND_OFFLINE_COLLECTION_REMOVE = "offline_collection_remove";
    public static final String KIND_OFFLINE_PLAYLIST_ADD = "offline_playlist_add";
    public static final String KIND_OFFLINE_PLAYLIST_REMOVE = "offline_playlist_remove";

    public static UIEvent fromPlayerOpen(String method) {
        return new UIEvent(KIND_PLAYER_OPEN)
                .put(LocalyticTrackingKeys.KEY_METHOD, method);
    }

    public static UIEvent fromPlayerClose(String method) {
        return new UIEvent(KIND_PLAYER_CLOSE)
                .put(LocalyticTrackingKeys.KEY_METHOD, method);
    }

    public static UIEvent fromToggleFollow(boolean isFollow, @NonNull EntityMetadata userMetadata) {
        return new UIEvent(isFollow ? KIND_FOLLOW : KIND_UNFOLLOW)
                .putPlayableMetadata(userMetadata);
    }

    public static UIEvent fromToggleLike(boolean isLike,
                                         @NonNull Urn resourceUrn,
                                         @NonNull EventContextMetadata contextMetadata,
                                         @Nullable PromotedSourceInfo promotedSourceInfo,
                                         @NonNull EntityMetadata playable) {
        return new UIEvent(isLike ? KIND_LIKE : KIND_UNLIKE)
                .<UIEvent>put(LocalyticTrackingKeys.KEY_RESOURCES_TYPE, getPlayableType(resourceUrn))
                .<UIEvent>put(LocalyticTrackingKeys.KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()))
                .<UIEvent>put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
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
                .<UIEvent>put(LocalyticTrackingKeys.KEY_RESOURCES_TYPE, getPlayableType(resourceUrn))
                .<UIEvent>put(LocalyticTrackingKeys.KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()))
                .<UIEvent>put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .putEventContextMetadata(contextMetadata)
                .putPromotedItemKeys(promotedSourceInfo)
                .putPlayableMetadata(entityMetadata);
    }

    public static UIEvent fromAddToPlaylist(EventContextMetadata eventContextMetadata, boolean isNewPlaylist, long trackId) {
        return new UIEvent(KIND_ADD_TO_PLAYLIST)
                .putEventContextMetadata(eventContextMetadata)
                .put(LocalyticTrackingKeys.KEY_IS_NEW_PLAYLIST, isNewPlaylist ? "yes" : "no")
                .put(LocalyticTrackingKeys.KEY_TRACK_ID, String.valueOf(trackId));
    }

    public static UIEvent fromComment(EventContextMetadata eventContextMetadata, long trackId, @NonNull EntityMetadata playable) {
        return new UIEvent(KIND_COMMENT)
                .<UIEvent>put(LocalyticTrackingKeys.KEY_TRACK_ID, String.valueOf(trackId))
                .putEventContextMetadata(eventContextMetadata)
                .putPlayableMetadata(playable);
    }

    public static UIEvent fromShare(@NonNull Urn resourceUrn,
                                    @NonNull EventContextMetadata contextMetadata,
                                    @Nullable PromotedSourceInfo promotedSourceInfo,
                                    @NonNull EntityMetadata playable) {
        return new UIEvent(KIND_SHARE)
                .<UIEvent>put(LocalyticTrackingKeys.KEY_RESOURCES_TYPE, getPlayableType(resourceUrn))
                .<UIEvent>put(LocalyticTrackingKeys.KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()))
                .<UIEvent>put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .putEventContextMetadata(contextMetadata)
                .putPromotedItemKeys(promotedSourceInfo)
                .putPlayableMetadata(playable);
    }

    public static UIEvent fromShuffleMyLikes() {
        return new UIEvent(KIND_SHUFFLE_LIKES);
    }

    public static UIEvent fromShufflePlaylist(String screenTag, @NonNull Urn resourceUrn) {
        return new UIEvent(KIND_SHUFFLE_PLAYLIST)
                .put(LocalyticTrackingKeys.KEY_CONTEXT, screenTag)
                .put(LocalyticTrackingKeys.KEY_RESOURCES_TYPE, TYPE_PLAYLIST)
                .put(LocalyticTrackingKeys.KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()));
    }

    public static UIEvent fromProfileNav() {
        return new UIEvent(KIND_NAVIGATION).put(LocalyticTrackingKeys.KEY_PAGE, "you");
    }

    public static UIEvent fromStreamNav() {
        return new UIEvent(KIND_NAVIGATION).put(LocalyticTrackingKeys.KEY_PAGE, "stream");
    }

    public static UIEvent fromExploreNav() {
        return new UIEvent(KIND_NAVIGATION).put(LocalyticTrackingKeys.KEY_PAGE, "explore");
    }

    public static UIEvent fromLikesNav() {
        return new UIEvent(KIND_NAVIGATION).put(LocalyticTrackingKeys.KEY_PAGE, "collection_likes");
    }

    public static UIEvent fromPlaylistsNav() {
        return new UIEvent(KIND_NAVIGATION).put(LocalyticTrackingKeys.KEY_PAGE, "collection_playlists");
    }

    public static UIEvent fromSearchAction() {
        return new UIEvent(KIND_NAVIGATION).put(LocalyticTrackingKeys.KEY_PAGE, "search");
    }

    public static UIEvent fromRemoveOfflineLikes(String pageName) {
        return new UIEvent(KIND_OFFLINE_LIKES_REMOVE)
                .put(AdTrackingKeys.KEY_ORIGIN_SCREEN, pageName);
    }

    public static UIEvent fromAddOfflineLikes(String pageName) {
        return new UIEvent(KIND_OFFLINE_LIKES_ADD)
                .put(AdTrackingKeys.KEY_ORIGIN_SCREEN, pageName);
    }

    public static UIEvent fromToggleOfflineCollection(boolean addToOffline) {
        return new UIEvent(addToOffline ? KIND_OFFLINE_COLLECTION_ADD : KIND_OFFLINE_COLLECTION_REMOVE);
    }

    public static UIEvent fromRemoveOfflinePlaylist(String pageName, @NonNull Urn resourceUrn,
                                                    @Nullable PromotedSourceInfo promotedSourceInfo) {
        return new UIEvent(KIND_OFFLINE_PLAYLIST_REMOVE)
                .<UIEvent>put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .<UIEvent>put(AdTrackingKeys.KEY_ORIGIN_SCREEN, pageName)
                .putPromotedItemKeys(promotedSourceInfo);
    }

    public static UIEvent fromAddOfflinePlaylist(String pageName, @NonNull Urn resourceUrn,
                                                 @Nullable PromotedSourceInfo promotedSourceInfo) {
        return new UIEvent(KIND_OFFLINE_PLAYLIST_ADD)
                .<UIEvent>put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .<UIEvent>put(AdTrackingKeys.KEY_ORIGIN_SCREEN, pageName)
                .putPromotedItemKeys(promotedSourceInfo);
    }

    public static UIEvent fromAudioAdClick(AudioAd audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo) {
        return fromAudioAdCompanionDisplayClick(audioAd, audioAdTrack, user, trackSourceInfo, System.currentTimeMillis());
    }

    public static UIEvent fromSkipAudioAdClick(AudioAd audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo) {
        return fromSkipAudioAdClick(audioAd, audioAdTrack, user, trackSourceInfo, System.currentTimeMillis());
    }

    @VisibleForTesting
    public static UIEvent fromAudioAdCompanionDisplayClick(AudioAd audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo, long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(KIND_AUDIO_AD_CLICK, timestamp), audioAd, audioAdTrack, user, trackSourceInfo)
                .<UIEvent>put(AdTrackingKeys.KEY_AD_URN, audioAd.getVisualAd().getAdUrn().toString())
                .<UIEvent>put(AdTrackingKeys.KEY_AD_ARTWORK_URL, audioAd.getVisualAd().getImageUrl().toString())
                .<UIEvent>put(AdTrackingKeys.KEY_CLICK_THROUGH_URL, audioAd.getVisualAd().getClickThroughUrl().toString())
                .addPromotedTrackingUrls(CLICKTHROUGHS, audioAd.getVisualAd().getClickUrls());
    }

    @VisibleForTesting
    public static UIEvent fromSkipAudioAdClick(AudioAd audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo, long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(KIND_SKIP_AUDIO_AD_CLICK, timestamp), audioAd, audioAdTrack, user, trackSourceInfo)
                .<UIEvent>put(AdTrackingKeys.KEY_AD_URN, audioAd.getAdUrn().toString())
                .addPromotedTrackingUrls(SKIPS, audioAd.getSkipUrls());
    }

    public static UIEvent fromCreatePlaylist(EntityMetadata metadata) {
        return new UIEvent(KIND_CREATE_PLAYLIST)
                .putPlayableMetadata(metadata);
    }

    private static UIEvent withBasicAudioAdAttributes(UIEvent event, AudioAd audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo) {
        return event
                .put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, audioAdTrack.toString())
                .put(AdTrackingKeys.KEY_USER_URN, user.toString())
                .put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, audioAd.getMonetizableTrackUrn().toString())
                .put(AdTrackingKeys.KEY_AD_ARTWORK_URL, audioAd.getVisualAd().getImageUrl().toString())
                .put(AdTrackingKeys.KEY_AD_TRACK_URN, audioAdTrack.toString())
                .put(AdTrackingKeys.KEY_ORIGIN_SCREEN, getNotNullOriginScreen(trackSourceInfo));
    }

    private static String getNotNullOriginScreen(@Nullable TrackSourceInfo trackSourceInfo) {
        if (trackSourceInfo != null) {
            return trackSourceInfo.getOriginScreen();
        }
        return ScTextUtils.EMPTY_STRING;
    }

    private static String getPlayableType(Urn resourceUrn) {
        if (resourceUrn.isTrack()) {
            return TYPE_TRACK;
        } else if (resourceUrn.isPlaylist()) {
            return TYPE_PLAYLIST;
        } else {
            return TYPE_UNKNOWN;
        }
    }

    public UIEvent(String kind) {
        this(kind, System.currentTimeMillis());
    }

    public UIEvent(String kind, long timeStamp) {
        super(kind, timeStamp);
        promotedTrackingUrls = new HashMap<>();
    }

    public UIEvent putPromotedItemKeys(@Nullable PromotedSourceInfo promotedSourceInfo) {
        if (promotedSourceInfo != null) {
            this.put(AdTrackingKeys.KEY_AD_URN, promotedSourceInfo.getAdUrn())
                    .put(AdTrackingKeys.KEY_MONETIZATION_TYPE, TYPE_MONETIZABLE_PROMOTED);

            if (promotedSourceInfo.getPromoterUrn().isPresent()) {
                this.put(AdTrackingKeys.KEY_PROMOTER_URN, promotedSourceInfo.getPromoterUrn().get().toString());
            }
        }
        return this;
    }

    private UIEvent putEventContextMetadata(@NonNull EventContextMetadata contextMetadata) {
        this.eventContextMetadata = contextMetadata;

        put(AdTrackingKeys.KEY_PAGE_URN, contextMetadata.pageUrn().toString())
                .put(AdTrackingKeys.KEY_ORIGIN_SCREEN, contextMetadata.pageName());
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
            return StringUtils.EMPTY_STRING;
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

    public List<String> getAudioAdClickthroughUrls() {
        List<String> urls = promotedTrackingUrls.get(CLICKTHROUGHS);
        return urls == null ? Collections.<String>emptyList() : urls;
    }

    public List<String> getAudioAdSkipUrls() {
        List<String> urls = promotedTrackingUrls.get(SKIPS);
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
