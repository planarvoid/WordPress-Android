package com.soundcloud.android.events;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UIEvent {

    public static final String METHOD_TAP_FOOTER = "tap_footer";
    public static final String METHOD_HIDE_BUTTON = "hide_button";
    public static final String METHOD_BACK_BUTTON = "back_button";
    public static final String METHOD_CONTENT_INTERACTION = "content_interaction";
    public static final String METHOD_TRACK_PLAY = "track_play";
    public static final String METHOD_AD_PLAY = "ad_play";

    private static final String CLICKTHROUGHS = "CLICKTHROUGHS";
    private static final String SKIPS = "SKIPS";

    private final Kind kind;
    private final Map<String, String> attributes;
    private final Map<String, List<String>> promotedTrackingUrls;

    private final long timestamp;

    public enum Kind {
        FOLLOW,
        UNFOLLOW,
        LIKE,
        UNLIKE,
        REPOST,
        UNREPOST,
        ADD_TO_PLAYLIST,
        COMMENT,
        SHARE,
        SHUFFLE_LIKES,
        NAVIGATION,
        AUDIO_AD_CLICK,
        SKIP_AUDIO_AD_CLICK,
        PLAYER_OPEN,
        PLAYER_CLOSE
    }

    public static UIEvent fromPlayerOpen(String method) {
        return new UIEvent(Kind.PLAYER_OPEN)
                .putAttribute("method", method);
    }

    public static UIEvent fromPlayerClose(String method) {
        return new UIEvent(Kind.PLAYER_CLOSE)
                .putAttribute("method", method);
    }

    public static UIEvent fromToggleFollow(boolean isFollow, String screenTag, long userId) {
        return new UIEvent(isFollow ? Kind.FOLLOW : Kind.UNFOLLOW)
                .putAttribute("context", screenTag)
                .putAttribute("user_id", String.valueOf(userId));
    }

    public static UIEvent fromToggleLike(boolean isLike, String screenTag, @NotNull Playable playable) {
        return new UIEvent(isLike ? Kind.LIKE : Kind.UNLIKE)
                .putAttribute("context", screenTag)
                .putAttribute("resource", getPlayableType(playable))
                .putAttribute("resource_id", String.valueOf(playable.getId()));
    }

    public static UIEvent fromToggleRepost(boolean isRepost, String screenTag, @NotNull Playable playable) {
        return new UIEvent(isRepost ? Kind.REPOST : Kind.UNREPOST)
                .putAttribute("context", screenTag)
                .putAttribute("resource", getPlayableType(playable))
                .putAttribute("resource_id", String.valueOf(playable.getId()));
    }

    public static UIEvent fromAddToPlaylist(String screenTag, boolean isNewPlaylist, long trackId) {
        return new UIEvent(Kind.ADD_TO_PLAYLIST)
                .putAttribute("context", screenTag)
                .putAttribute("is_new_playlist", isNewPlaylist ? "yes" : "no")
                .putAttribute("track_id", String.valueOf(trackId));
    }

    public static UIEvent fromComment(String screenTag, long trackId) {
        return new UIEvent(Kind.COMMENT)
                .putAttribute("context", screenTag)
                .putAttribute("track_id", String.valueOf(trackId));
    }

    public static UIEvent fromShare(String screenTag, @NotNull Playable playable) {
        return new UIEvent(Kind.SHARE)
                .putAttribute("context", screenTag)
                .putAttribute("resource", getPlayableType(playable))
                .putAttribute("resource_id", String.valueOf(playable.getId()));
    }

    public static UIEvent fromShuffleMyLikes() {
        return new UIEvent(Kind.SHUFFLE_LIKES);
    }

    public static UIEvent fromProfileNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "you");
    }

    public static UIEvent fromStreamNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "stream");
    }

    public static UIEvent fromExploreNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "explore");
    }

    public static UIEvent fromLikesNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "collection_likes");
    }

    public static UIEvent fromPlaylistsNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "collection_playlists");
    }

    public static UIEvent fromSearchAction() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "search");
    }

    @VisibleForTesting
    public static UIEvent fromAudioAdCompanionDisplayClick(PropertySet audioAd, TrackUrn audioAdTrack, long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(Kind.AUDIO_AD_CLICK, timestamp), audioAd, audioAdTrack)
                .putAttribute("ad_click_url", audioAd.get(AdProperty.CLICK_THROUGH_LINK).toString())
                .addPromotedTrackingUrls(CLICKTHROUGHS, audioAd.get(AdProperty.AUDIO_AD_CLICKTHROUGH_URLS));
    }

    @VisibleForTesting
    public static UIEvent fromSkipAudioAdClick(PropertySet audioAd, TrackUrn audioAdTrack, long timestamp) {
        return withBasicAudioAdAttributes(new UIEvent(Kind.SKIP_AUDIO_AD_CLICK, timestamp), audioAd, audioAdTrack)
                .addPromotedTrackingUrls(SKIPS, audioAd.get(AdProperty.AUDIO_AD_SKIP_URLS));
    }

    private static UIEvent withBasicAudioAdAttributes(UIEvent event, PropertySet audioAd, TrackUrn audioAdTrack) {
        return event.putAttribute("ad_urn", audioAd.get(AdProperty.AD_URN))
                .putAttribute("ad_monetized_urn", audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .putAttribute("ad_image_url", audioAd.get(AdProperty.ARTWORK).toString())
                .putAttribute("ad_track_urn", audioAdTrack.toString());
    }

    public static UIEvent fromAudioAdClick(PropertySet audioAd, TrackUrn audioAdTrack) {
        return fromAudioAdCompanionDisplayClick(audioAd, audioAdTrack, System.currentTimeMillis());
    }

    public static UIEvent fromSkipAudioAdClick(PropertySet audioAd, TrackUrn audioAdTrack) {
        return fromSkipAudioAdClick(audioAd, audioAdTrack, System.currentTimeMillis());
    }

    private static String getPlayableType(Playable playable) {
        return playable instanceof PublicApiTrack ? "track" : "playlist";
    }

    public UIEvent(Kind kind) {
        this(kind, System.currentTimeMillis());
    }

    public UIEvent(Kind kind, long timestamp) {
        this.kind = kind;
        this.timestamp = timestamp;
        attributes = new HashMap<String, String>();
        promotedTrackingUrls = new HashMap<String, List<String>>();
    }

    public Kind getKind() {
        return kind;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getAttributes() {
        return attributes;
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

    private UIEvent putAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    private UIEvent addPromotedTrackingUrls(String key, List<String> urls) {
        promotedTrackingUrls.put(key, urls);
        return this;
    }
}
