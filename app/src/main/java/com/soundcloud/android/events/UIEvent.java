package com.soundcloud.android.events;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private static final String KEY_METHOD = "method";
    private static final String KEY_CONTEXT = "context";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_RESOURCES_TYPE = "resource";
    private static final String KEY_RESOURCE_ID = "resource_id";
    private static final String KEY_IS_NEW_PLAYLIST = "is_new_playlist";
    private static final String KEY_TRACK_ID = "track_id";
    private static final String KEY_AD_CLICK_URL = "ad_click_url";
    private static final String KEY_AD_URN = "ad_urn";
    private static final String KEY_AD_MONETIZED_URN = "ad_monetized_urn";
    private static final String KEY_AD_IMAGE_URL = "ad_image_url";
    private static final String KEY_AD_TRACK_URN = "ad_track_urn";
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
    public static final String KIND_NAVIGATION = "navigation";
    public static final String KIND_AUDIO_AD_CLICK = "audio_ad_click";
    public static final String KIND_SKIP_AUDIO_AD_CLICK = "skip_audio_ad_click";
    public static final String KIND_PLAYER_OPEN = "player_open";
    public static final String KIND_PLAYER_CLOSE = "player_close";

    public static UIEvent fromPlayerOpen(String method) {
        return new UIEvent(KIND_PLAYER_OPEN)
                .put(KEY_METHOD, method);
    }

    public static UIEvent fromPlayerClose(String method) {
        return new UIEvent(KIND_PLAYER_CLOSE)
                .put(KEY_METHOD, method);
    }

    public static UIEvent fromToggleFollow(boolean isFollow, String screenTag, long userId) {
        return new UIEvent(isFollow ? KIND_FOLLOW : KIND_UNFOLLOW)
                .put(KEY_CONTEXT, screenTag)
                .put(KEY_USER_ID, String.valueOf(userId));
    }

    public static UIEvent fromToggleLike(boolean isLike, String screenTag, @NotNull Urn resourceUrn) {
        return new UIEvent(isLike ? KIND_LIKE : KIND_UNLIKE)
                .put(KEY_CONTEXT, screenTag)
                .put(KEY_RESOURCES_TYPE, getPlayableType(resourceUrn))
                .put(KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()));
    }

    public static UIEvent fromToggleRepost(boolean isRepost, String screenTag, @NotNull Urn resourceUrn) {
        return new UIEvent(isRepost ? KIND_REPOST : KIND_UNREPOST)
                .put(KEY_CONTEXT, screenTag)
                .put(KEY_RESOURCES_TYPE, getPlayableType(resourceUrn))
                .put(KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()));
    }

    public static UIEvent fromAddToPlaylist(String screenTag, boolean isNewPlaylist, long trackId) {
        return new UIEvent(KIND_ADD_TO_PLAYLIST)
                .put(KEY_CONTEXT, screenTag)
                .put(KEY_IS_NEW_PLAYLIST, isNewPlaylist ? "yes" : "no")
                .put(KEY_TRACK_ID, String.valueOf(trackId));
    }

    public static UIEvent fromComment(String screenTag, long trackId) {
        return new UIEvent(KIND_COMMENT)
                .put(KEY_CONTEXT, screenTag)
                .put(KEY_TRACK_ID, String.valueOf(trackId));
    }

    public static UIEvent fromShare(String screenTag, @NotNull Urn resourceUrn) {
        return new UIEvent(KIND_SHARE)
                .put(KEY_CONTEXT, screenTag)
                .put(KEY_RESOURCES_TYPE, getPlayableType(resourceUrn))
                .put(KEY_RESOURCE_ID, String.valueOf(resourceUrn.getNumericId()));
    }

    public static UIEvent fromShuffleMyLikes() {
        return new UIEvent(KIND_SHUFFLE_LIKES);
    }

    public static UIEvent fromProfileNav() {
        return new UIEvent(KIND_NAVIGATION).put("page", "you");
    }

    public static UIEvent fromStreamNav() {
        return new UIEvent(KIND_NAVIGATION).put("page", "stream");
    }

    public static UIEvent fromExploreNav() {
        return new UIEvent(KIND_NAVIGATION).put("page", "explore");
    }

    public static UIEvent fromLikesNav() {
        return new UIEvent(KIND_NAVIGATION).put("page", "collection_likes");
    }

    public static UIEvent fromPlaylistsNav() {
        return new UIEvent(KIND_NAVIGATION).put("page", "collection_playlists");
    }

    public static UIEvent fromSearchAction() {
        return new UIEvent(KIND_NAVIGATION).put("page", "search");
    }

    @VisibleForTesting
    public static UIEvent fromAudioAdCompanionDisplayClick(PropertySet audioAd, Urn audioAdTrack, long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(KIND_AUDIO_AD_CLICK, timestamp), audioAd, audioAdTrack)
                .put(KEY_AD_CLICK_URL, audioAd.get(AdProperty.CLICK_THROUGH_LINK).toString())
                .addPromotedTrackingUrls(CLICKTHROUGHS, audioAd.get(AdProperty.AUDIO_AD_CLICKTHROUGH_URLS));
    }

    @VisibleForTesting
    public static UIEvent fromSkipAudioAdClick(PropertySet audioAd, Urn audioAdTrack, long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(KIND_SKIP_AUDIO_AD_CLICK, timestamp), audioAd, audioAdTrack)
                .addPromotedTrackingUrls(SKIPS, audioAd.get(AdProperty.AUDIO_AD_SKIP_URLS));
    }

    private static UIEvent withBasicAudioAdAttributes(UIEvent event, PropertySet audioAd, Urn audioAdTrack) {
        return event.put(KEY_AD_URN, audioAd.get(AdProperty.AD_URN))
                .put(KEY_AD_MONETIZED_URN, audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .put(KEY_AD_IMAGE_URL, audioAd.get(AdProperty.ARTWORK).toString())
                .put(KEY_AD_TRACK_URN, audioAdTrack.toString());
    }

    public static UIEvent fromAudioAdClick(PropertySet audioAd, Urn audioAdTrack) {
        return fromAudioAdCompanionDisplayClick(audioAd, audioAdTrack, System.currentTimeMillis());
    }

    public static UIEvent fromSkipAudioAdClick(PropertySet audioAd, Urn audioAdTrack) {
        return fromSkipAudioAdClick(audioAd, audioAdTrack, System.currentTimeMillis());
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
