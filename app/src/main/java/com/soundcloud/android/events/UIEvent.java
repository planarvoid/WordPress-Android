package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static final String KIND_FOLLOW = "follow";
    public static final String KIND_UNFOLLOW = "unfollow";
    public static final String KIND_LIKE = "like";
    public static final String KIND_UNLIKE = "unlike";
    public static final String KIND_REPOST = "repost";
    public static final String KIND_UNREPOST = "unrepost";
    public static final String KIND_ADD_TO_PLAYLIST = "add_to_playlist";
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

    public static UIEvent fromToggleFollow(boolean isFollow, String screenTag, PropertySet user) {
        return new UIEvent(isFollow ? KIND_FOLLOW : KIND_UNFOLLOW)
                .put(LocalyticTrackingKeys.KEY_CONTEXT, screenTag)
                .put(LocalyticTrackingKeys.KEY_USER_ID, String.valueOf(user.get(UserProperty.ID)))
                .putCreatorPropertyKeys(user);
    }

    public static UIEvent fromToggleLike(boolean isLike,
                                         String invokerScreen,
                                         String contextScreen,
                                         String pageName,
                                         @NotNull Urn resourceUrn,
                                         @NotNull Urn pageUrn,
                                         @Nullable PromotedSourceInfo promotedSourceInfo,
                                         @Nullable PlayableItem playableItem) {
        return new UIEvent(isLike ? KIND_LIKE : KIND_UNLIKE)
                .put(LocalyticTrackingKeys.KEY_LOCATION, invokerScreen)
                .put(LocalyticTrackingKeys.KEY_CONTEXT, contextScreen)
                .put(LocalyticTrackingKeys.KEY_RESOURCES_TYPE, getPlayableType(resourceUrn))
                .put(LocalyticTrackingKeys.KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()))
                .put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .put(AdTrackingKeys.KEY_PAGE_URN, pageUrn.toString())
                .put(AdTrackingKeys.KEY_ORIGIN_SCREEN, pageName)
                .putPromotedItemKeys(promotedSourceInfo)
                .putPlayableItemKeys(playableItem);
    }

    public static UIEvent fromToggleRepost(boolean isRepost,
                                           String screenTag,
                                           String pageName,
                                           @NotNull Urn resourceUrn,
                                           @NotNull Urn pageUrn,
                                           @Nullable PromotedSourceInfo promotedSourceInfo) {
        return new UIEvent(isRepost ? KIND_REPOST : KIND_UNREPOST)
                .put(LocalyticTrackingKeys.KEY_CONTEXT, screenTag)
                .put(LocalyticTrackingKeys.KEY_RESOURCES_TYPE, getPlayableType(resourceUrn))
                .put(LocalyticTrackingKeys.KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()))
                .put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .put(AdTrackingKeys.KEY_PAGE_URN, pageUrn.toString())
                .put(AdTrackingKeys.KEY_ORIGIN_SCREEN, pageName)
                .putPromotedItemKeys(promotedSourceInfo);
    }

    public static UIEvent fromAddToPlaylist(String invokerScreen, String contextScreen, boolean isNewPlaylist, long trackId) {
        return new UIEvent(KIND_ADD_TO_PLAYLIST)
                .put(LocalyticTrackingKeys.KEY_LOCATION, invokerScreen)
                .put(LocalyticTrackingKeys.KEY_CONTEXT, contextScreen)
                .put(LocalyticTrackingKeys.KEY_IS_NEW_PLAYLIST, isNewPlaylist ? "yes" : "no")
                .put(LocalyticTrackingKeys.KEY_TRACK_ID, String.valueOf(trackId));
    }

    public static UIEvent fromComment(String screenTag, long trackId, @Nullable PropertySet track) {
        return new UIEvent(KIND_COMMENT)
                .put(LocalyticTrackingKeys.KEY_CONTEXT, screenTag)
                .put(LocalyticTrackingKeys.KEY_TRACK_ID, String.valueOf(trackId))
                .putPlayablePropertySetKeys(track);
    }

    public static UIEvent fromShare(String screenTag, @NotNull Urn resourceUrn, @NotNull PropertySet playable) {
        return new UIEvent(KIND_SHARE)
                .put(LocalyticTrackingKeys.KEY_CONTEXT, screenTag)
                .put(LocalyticTrackingKeys.KEY_RESOURCES_TYPE, getPlayableType(resourceUrn))
                .put(LocalyticTrackingKeys.KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()))
                .putPlayablePropertySetKeys(playable);
    }

    public static UIEvent fromShuffleMyLikes() {
        return new UIEvent(KIND_SHUFFLE_LIKES);
    }

    public static UIEvent fromShufflePlaylist(String screenTag, @NotNull Urn resourceUrn) {
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

    public static UIEvent fromRemoveOfflinePlaylist(String pageName, @NotNull Urn resourceUrn,
                                                    @Nullable PromotedSourceInfo promotedSourceInfo) {
        return new UIEvent(KIND_OFFLINE_PLAYLIST_REMOVE)
                .put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .put(AdTrackingKeys.KEY_ORIGIN_SCREEN, pageName)
                .putPromotedItemKeys(promotedSourceInfo);
    }

    public static UIEvent fromAddOfflinePlaylist(String pageName, @NotNull Urn resourceUrn,
                                                 @Nullable PromotedSourceInfo promotedSourceInfo) {
        return new UIEvent(KIND_OFFLINE_PLAYLIST_ADD)
                .put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, resourceUrn.toString())
                .put(AdTrackingKeys.KEY_ORIGIN_SCREEN, pageName)
                .putPromotedItemKeys(promotedSourceInfo);
    }

    public static UIEvent fromAudioAdClick(PropertySet audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo) {
        return fromAudioAdCompanionDisplayClick(audioAd, audioAdTrack, user, trackSourceInfo, System.currentTimeMillis());
    }

    public static UIEvent fromSkipAudioAdClick(PropertySet audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo) {
        return fromSkipAudioAdClick(audioAd, audioAdTrack, user, trackSourceInfo, System.currentTimeMillis());
    }

    @VisibleForTesting
    public static UIEvent fromAudioAdCompanionDisplayClick(PropertySet audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo, long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(KIND_AUDIO_AD_CLICK, timestamp), audioAd, audioAdTrack, user, trackSourceInfo)
                .put(AdTrackingKeys.KEY_AD_URN, audioAd.get(AdProperty.COMPANION_URN))
                .put(AdTrackingKeys.KEY_AD_ARTWORK_URL, audioAd.get(AdProperty.ARTWORK).toString())
                .put(AdTrackingKeys.KEY_CLICK_THROUGH_URL, audioAd.get(AdProperty.CLICK_THROUGH_LINK).toString())
                .addPromotedTrackingUrls(CLICKTHROUGHS, audioAd.get(AdProperty.AD_CLICKTHROUGH_URLS));
    }

    @VisibleForTesting
    public static UIEvent fromSkipAudioAdClick(PropertySet audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo, long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(KIND_SKIP_AUDIO_AD_CLICK, timestamp), audioAd, audioAdTrack, user, trackSourceInfo)
                .put(AdTrackingKeys.KEY_AD_URN, audioAd.get(AdProperty.AD_URN))
                .addPromotedTrackingUrls(SKIPS, audioAd.get(AdProperty.AD_SKIP_URLS));
    }

    private static UIEvent withBasicAudioAdAttributes(UIEvent event, PropertySet audioAd, Urn audioAdTrack, Urn user, @Nullable TrackSourceInfo trackSourceInfo) {
        return event
                .put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, audioAdTrack.toString())
                .put(AdTrackingKeys.KEY_USER_URN, user.toString())
                .put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .put(AdTrackingKeys.KEY_AD_ARTWORK_URL, audioAd.get(AdProperty.ARTWORK).toString())
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

    @Override
    public UIEvent put(String key, @Nullable String value) {
        return (UIEvent) super.put(key, value);
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

    private UIEvent putPlayableItemKeys(@Nullable PlayableItem playableItem) {
        PlayableMetadata
                .fromPlayableItem(playableItem)
                .addToTrackingEvent(this);
        return this;
    }

    private UIEvent putCreatorPropertyKeys(PropertySet user) {
        PlayableMetadata
                .fromUserProperties(user)
                .addToTrackingEvent(this);
        return this;
    }

    private UIEvent putPlayablePropertySetKeys(PropertySet properties) {
        PlayableMetadata
                .fromPlayableProperties(properties)
                .addToTrackingEvent(this);
        return this;
    }

    public List<String> getAudioAdClickthroughUrls() {
        List<String> urls = promotedTrackingUrls.get(CLICKTHROUGHS);
        return urls == null ? Collections.<String>emptyList() : urls;
    }

    public List<String> getAudioAdSkipUrls() {
        List<String> urls = promotedTrackingUrls.get(SKIPS);
        return urls == null ? Collections.<String>emptyList() : urls;
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
