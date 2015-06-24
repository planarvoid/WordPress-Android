package com.soundcloud.android.testsupport.fixtures;

import static com.google.common.collect.Lists.newArrayList;

import com.google.common.base.Optional;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.InterstitialProperty;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.ModelFixtures;
import com.soundcloud.android.tracks.PromotedTrackProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;

import java.util.Arrays;
import java.util.Date;

public abstract class TestPropertySets {
    public static PropertySet audioAdProperties(Urn monetizedTrack) {
        return PropertySet.from(
                AdProperty.AUDIO_AD_URN.bind("ad:audio:123"),
                AdProperty.COMPANION_URN.bind("ad:visual:123"),
                AdProperty.MONETIZABLE_TRACK_URN.bind(monetizedTrack),
                AdProperty.ARTWORK.bind(Uri.parse("http:a//d.artwork.url")),
                AdProperty.CLICK_THROUGH_LINK.bind(Uri.parse("http://ad.click.through.url")),
                AdProperty.DEFAULT_TEXT_COLOR.bind("#000000"),
                AdProperty.DEFAULT_BACKGROUND_COLOR.bind("#FFFFF"),
                AdProperty.PRESSED_TEXT_COLOR.bind("#111111"),
                AdProperty.PRESSED_BACKGROUND_COLOR.bind("#222222"),
                AdProperty.FOCUSED_TEXT_COLOR.bind("#333333"),
                AdProperty.FOCUSED_BACKGROUND_COLOR.bind("#444444"),
                AdProperty.AUDIO_AD_IMPRESSION_URLS.bind(newArrayList("adswizzUrl", "advertiserUrl")),
                AdProperty.AUDIO_AD_FINISH_URLS.bind(newArrayList("finish1", "finish2")),
                AdProperty.AUDIO_AD_CLICKTHROUGH_URLS.bind(newArrayList("click1", "click2")),
                AdProperty.AUDIO_AD_SKIP_URLS.bind(newArrayList("skip1", "skip2")),
                AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS.bind(newArrayList("visual1", "visual2"))
        );
    }

    public static PropertySet expectedTrackForWidget() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.TITLE.bind("someone's favorite song"),
                PlayableProperty.CREATOR_NAME.bind("someone's favorite band"),
                PlayableProperty.CREATOR_URN.bind(Urn.forUser(123L)),
                PlayableProperty.IS_LIKED.bind(false)
        );
    }

    public static PropertySet expectedTrackForPlayer() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                TrackProperty.WAVEFORM_URL.bind("http://waveform.url"),
                TrackProperty.PLAY_COUNT.bind(1),
                TrackProperty.COMMENTS_COUNT.bind(1),
                TrackProperty.STREAM_URL.bind("http://stream.url"),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind("squirlex"),
                PlayableProperty.CREATOR_URN.bind(Urn.forUser(456L)),
                PlayableProperty.DURATION.bind(20000L),
                PlayableProperty.IS_LIKED.bind(true),
                PlayableProperty.LIKES_COUNT.bind(1),
                PlayableProperty.REPOSTS_COUNT.bind(1),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_PRIVATE.bind(false),
                PlayableProperty.IS_REPOSTED.bind(false),
                PlayableProperty.CREATED_AT.bind(new Date())
        );
    }

    public static PropertySet leaveBehindForPlayer() {
        return PropertySet.from(
                LeaveBehindProperty.AUDIO_AD_TRACK_URN.bind(Urn.forTrack(123L)),
                LeaveBehindProperty.LEAVE_BEHIND_URN.bind("adswizz:leavebehind:1105"),
                LeaveBehindProperty.IMAGE_URL.bind("https://va.sndcdn.com/mlb/sqsp-example-leave-behind.jpg"),
                LeaveBehindProperty.CLICK_THROUGH_URL.bind(Uri.parse("http://squarespace.com")),
                LeaveBehindProperty.TRACKING_IMPRESSION_URLS.bind(newArrayList("leaveBehindTrackingImpressionUrl1", "leaveBehindTrackingImpressionUrl2")),
                LeaveBehindProperty.TRACKING_CLICK_URLS.bind(newArrayList("leaveBehindTrackingClickTroughUrl1", "leaveBehindTrackingClickTroughUrl2"))
        );
    }

    public static PropertySet interstitialForPlayer() {
        return PropertySet.from(
                InterstitialProperty.INTERSTITIAL_URN.bind("adswizz:ads:1105"),
                InterstitialProperty.IMAGE_URL.bind("https://va.sndcdn.com/mlb/sqsp-example-leave-behind.jpg"),
                InterstitialProperty.CLICK_THROUGH_URL.bind(Uri.parse("http://squarespace.com")),
                InterstitialProperty.TRACKING_IMPRESSION_URLS.bind(newArrayList("https://promoted.soundcloud.com/impression?adData=instance%3Asoundcloud%3Bad_id%3A1105%3Bview_key%3A1410853892331806%3Bzone_id%3A56&loc=&listenerId=5284047f4ffb4e04824a2fd1d1f0cd62&sessionId=67fa476869b956676b5bae2866c377a9&ip=%3A%3Affff%3A80.82.202.196&OAGEO=ZGUlN0MxNiU3Q2JlcmxpbiU3QzEwMTE1JTdDNTIuNTMxOTk3NjgwNjY0MDYlN0MxMy4zOTIxOTY2NTUyNzM0MzglN0MlN0MlN0MlN0MlM0ElM0FmZmZmJTNBODAuODIuMjAyLjE5NiU3Q3RoZSt1bmJlbGlldmFibGUrbWFjaGluZStjb21wYW55K2dtYmg=&user_agent=SoundCloud-Android%2F14.09.02+%28Android+4.3%3B+Genymotion+Sony+Xperia+Z+-+4.3+-+API+18+-+1080x1920%29&cbs=681405")),
                InterstitialProperty.TRACKING_CLICK_URLS.bind(newArrayList("https://promoted.soundcloud.com/track?reqType=SCAdClicked&protocolVersion=2.0&adId=1105&zoneId=56&cb=dfd1b6e0c90745e9934f9d35b174ff30")),
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind("squirlex"));
    }

    public static PropertySet leaveBehindForPlayerWithDisplayMetaData() {
        return leaveBehindForPlayer()
                .put(LeaveBehindProperty.META_AD_COMPLETED, true)
                .put(LeaveBehindProperty.META_AD_CLICKED, false);
    }

    public static PropertySet expectedPrivateTrackForPlayer() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.IS_PRIVATE.bind(true),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind(""),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_REPOSTED.bind(true));
    }

    public static PropertySet expectedTrackForListItem(Urn urn) {
        return PropertySet.from(
                TrackProperty.URN.bind(urn),
                TrackProperty.TITLE.bind("Title " + urn),
                TrackProperty.CREATOR_NAME.bind("Creator " + urn),
                TrackProperty.DURATION.bind(10L),
                TrackProperty.PLAY_COUNT.bind(4),
                TrackProperty.LIKES_COUNT.bind(2),
                LikeProperty.CREATED_AT.bind(new Date()),
                TrackProperty.IS_PRIVATE.bind(false));
    }

    public static PropertySet expectedPromotedTrack() {
        return basePromotedTrack()
                .put(PromotedTrackProperty.PROMOTER_URN, Optional.of(Urn.forUser(193L)))
                .put(PromotedTrackProperty.PROMOTER_NAME, Optional.of("SoundCloud"));
    }

    public static PropertySet expectedPromotedTrackWithoutPromoter() {
        return basePromotedTrack()
                .put(PromotedTrackProperty.PROMOTER_URN, Optional.<Urn>absent())
                .put(PromotedTrackProperty.PROMOTER_NAME, Optional.<String>absent());
    }

    private static PropertySet basePromotedTrack() {
        return expectedTrackForListItem(Urn.forTrack(12345L))
                .put(PromotedTrackProperty.AD_URN, "ad:urn:123")
                .put(PromotedTrackProperty.CREATED_AT, new Date(Long.MAX_VALUE))
                .put(PromotedTrackProperty.TRACK_CLICKED_URLS, Arrays.asList("promoted1", "promoted2"))
                .put(PromotedTrackProperty.TRACK_IMPRESSION_URLS, Arrays.asList("promoted3", "promoted4"))
                .put(PromotedTrackProperty.TRACK_PLAYED_URLS, Arrays.asList("promoted5", "promoted6"))
                .put(PromotedTrackProperty.PROMOTER_CLICKED_URLS, Arrays.asList("promoted7", "promoted8"));
    }

    public static PropertySet expectedLikedTrackForLikesScreen() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                TrackProperty.TITLE.bind("squirlex galore"),
                TrackProperty.CREATOR_NAME.bind("avieciie"),
                TrackProperty.DURATION.bind(10L),
                TrackProperty.PLAY_COUNT.bind(4),
                TrackProperty.LIKES_COUNT.bind(2),
                LikeProperty.CREATED_AT.bind(new Date()),
                TrackProperty.IS_PRIVATE.bind(false));
    }

    public static PropertySet expectedLikedPlaylistForPlaylistsScreen() {
        return PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forPlaylist(123L)),
                PlaylistProperty.TITLE.bind("squirlex galore"),
                PlaylistProperty.CREATOR_NAME.bind("avieciie"),
                PlaylistProperty.TRACK_COUNT.bind(4),
                PlaylistProperty.LIKES_COUNT.bind(2),
                LikeProperty.CREATED_AT.bind(new Date()),
                PlaylistProperty.IS_PRIVATE.bind(false));
    }

    public static PropertySet expectedPostedPlaylistsForPostedPlaylistsScreen() {
        return PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forPlaylist(123L)),
                PlaylistProperty.TITLE.bind("squirlex galore"),
                PlaylistProperty.CREATOR_NAME.bind("avieciie"),
                PlaylistProperty.TRACK_COUNT.bind(4),
                PlaylistProperty.LIKES_COUNT.bind(2),
                PlaylistProperty.CREATED_AT.bind(new Date()),
                PlaylistProperty.IS_PRIVATE.bind(false));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Analytics / Tracking
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static PropertySet expectedTrackForAnalytics(Urn trackUrn, String policy, long duration) {
        return PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                TrackProperty.POLICY.bind(policy),
                PlayableProperty.DURATION.bind(duration)
        );
    }

    public static PropertySet expectedTrackForAnalytics(Urn trackUrn) {
        return expectedTrackForAnalytics(trackUrn, "allow", 1000);
    }

    public static PropertySet fromApiTrack(){
        return fromApiTrack(ModelFixtures.create(ApiTrack.class));
    }

    public static PropertySet fromApiTrack(ApiTrack apiTrack){
        return fromApiTrack(apiTrack, false, false, false);
    }

    public static PropertySet fromApiTrack(ApiTrack apiTrack, boolean isPrivate, boolean isLiked, boolean isReposted){
            return PropertySet.from(
                    TrackProperty.URN.bind(Urn.forTrack(apiTrack.getId())),
                    PlayableProperty.TITLE.bind(apiTrack.getTitle()),
                    PlayableProperty.DURATION.bind(apiTrack.getDuration()),
                    PlayableProperty.CREATOR_NAME.bind(apiTrack.getUser().getUsername()),
                    PlayableProperty.CREATOR_URN.bind(apiTrack.getUser().getUrn()),
                    TrackProperty.WAVEFORM_URL.bind(apiTrack.getWaveformUrl()),
                    TrackProperty.STREAM_URL.bind(apiTrack.getStreamUrl()),
                    TrackProperty.PLAY_COUNT.bind(apiTrack.getStats().getPlaybackCount()),
                    TrackProperty.COMMENTS_COUNT.bind(apiTrack.getStats().getCommentsCount()),
                    PlayableProperty.LIKES_COUNT.bind(apiTrack.getStats().getLikesCount()),
                    PlayableProperty.REPOSTS_COUNT.bind(apiTrack.getStats().getRepostsCount()),
                    TrackProperty.MONETIZABLE.bind(apiTrack.isMonetizable()),
                    TrackProperty.POLICY.bind(apiTrack.getPolicy()),
                    PlayableProperty.IS_LIKED.bind(isLiked),
                    PlayableProperty.PERMALINK_URL.bind(apiTrack.getPermalinkUrl()),
                    PlayableProperty.IS_PRIVATE.bind(isPrivate),
                    PlayableProperty.CREATED_AT.bind(apiTrack.getCreatedAt()),
                    PlayableProperty.IS_REPOSTED.bind(isReposted));
    }

    public static PropertySet likedEntityChangeSet(Urn targetUrn, int likesCount) {
        return PropertySet.from(
                PlayableProperty.URN.bind(targetUrn),
                PlayableProperty.LIKES_COUNT.bind(likesCount),
                PlayableProperty.IS_LIKED.bind(true)
        );
    }

    public static PropertySet unlikedEntityChangeSet(Urn targetUrn, int likesCount) {
        return PropertySet.from(
                PlayableProperty.URN.bind(targetUrn),
                PlayableProperty.LIKES_COUNT.bind(likesCount),
                PlayableProperty.IS_LIKED.bind(false)
        );
    }

    public static PropertySet fromApiPlaylist(ApiPlaylist apiPlaylist, boolean isLiked, boolean isReposted, boolean markedForOffline, boolean isPosted) {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forPlaylist(apiPlaylist.getId())),
                PlayableProperty.TITLE.bind(apiPlaylist.getTitle()),
                PlayableProperty.DURATION.bind(apiPlaylist.getDuration()),
                PlayableProperty.CREATOR_NAME.bind(apiPlaylist.getUser().getUsername()),
                PlayableProperty.CREATOR_URN.bind(apiPlaylist.getUser().getUrn()),
                PlayableProperty.LIKES_COUNT.bind(apiPlaylist.getStats().getLikesCount()),
                PlayableProperty.REPOSTS_COUNT.bind(apiPlaylist.getStats().getRepostsCount()),
                PlayableProperty.PERMALINK_URL.bind(apiPlaylist.getPermalinkUrl()),
                PlayableProperty.CREATED_AT.bind(apiPlaylist.getCreatedAt()),
                PlayableProperty.IS_PRIVATE.bind(Sharing.PRIVATE.equals(apiPlaylist.getSharing())),
                PlayableProperty.IS_LIKED.bind(isLiked),
                PlayableProperty.IS_REPOSTED.bind(isReposted),
                PlaylistProperty.IS_POSTED.bind(isPosted),
                OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE.bind(markedForOffline),
                PlaylistProperty.TRACK_COUNT.bind(apiPlaylist.getTrackCount()));
    }

    public static PropertySet followingEntityChangeSet(Urn targetUrn, int followersCount, boolean following) {
        return PropertySet.from(
                UserProperty.URN.bind(targetUrn),
                UserProperty.FOLLOWERS_COUNT.bind(followersCount),
                UserProperty.IS_FOLLOWED_BY_ME.bind(following)
        );
    }

    public static PropertySet userFollowing(ApiUser user, boolean following) {
        return PropertySet.from(
                UserProperty.URN.bind(Urn.forUser(user.getId())),
                UserProperty.USERNAME.bind(user.getUsername()),
                UserProperty.COUNTRY.bind(user.getCountry()),
                UserProperty.FOLLOWERS_COUNT.bind(user.getFollowersCount()),
                UserProperty.IS_FOLLOWED_BY_ME.bind(following));
    }
}
