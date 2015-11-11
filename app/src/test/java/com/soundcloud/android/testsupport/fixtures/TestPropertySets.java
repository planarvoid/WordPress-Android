package com.soundcloud.android.testsupport.fixtures;

import static java.util.Arrays.asList;

import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.InterstitialProperty;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.ads.VideoAdProperty;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserAssociationProperty;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import android.net.Uri;

import java.util.Date;

public abstract class TestPropertySets {

    public static PropertySet user() {
        return ModelFixtures.create(ApiUser.class).toPropertySet();
    }

    public static PropertySet videoAdProperties(Urn monetizedTrack) {
        return PropertySet.from(
                VideoAdProperty.AD_URN.bind("ad:video:123"),
                VideoAdProperty.AD_TYPE.bind(AdProperty.AD_TYPE_VIDEO),
                VideoAdProperty.MONETIZABLE_TRACK_URN.bind(monetizedTrack),
                VideoAdProperty.AD_IMPRESSION_URLS.bind(asList("impression1", "impression2")),
                VideoAdProperty.AD_SKIP_URLS.bind(asList("skip1", "skip2")),
                VideoAdProperty.AD_START_URLS.bind(asList("start1", "start2")),
                VideoAdProperty.AD_FIRST_QUARTILE_URLS.bind(asList("firstq")),
                VideoAdProperty.AD_SECOND_QUARTILE_URLS.bind(asList("secq")),
                VideoAdProperty.AD_THIRD_QUARTILE_URLS.bind(asList("thirdq")),
                VideoAdProperty.AD_FINISH_URLS.bind(asList("finish1", "finish2")),
                VideoAdProperty.AD_PAUSE_URLS.bind(asList("pause1", "pause2")),
                VideoAdProperty.AD_RESUME_URLS.bind(asList("resume1", "resume2")),
                VideoAdProperty.AD_FULLSCREEN_URLS.bind(asList("fullscreen")),
                VideoAdProperty.AD_EXIT_FULLSCREEN_URLS.bind(asList("exit-fullscreen")))
                .merge(companionDisplayProperties());
    }

    public static PropertySet audioAdProperties(Urn monetizedTrack) {
        return PropertySet.from(
                AdProperty.AD_URN.bind("ad:audio:123"),
                AdProperty.AD_TYPE.bind(AdProperty.AD_TYPE_AUDIO),
                AdProperty.MONETIZABLE_TRACK_URN.bind(monetizedTrack),
                AdProperty.AD_IMPRESSION_URLS.bind(asList("adswizzUrl", "advertiserUrl")),
                AdProperty.AD_FINISH_URLS.bind(asList("finish1", "finish2")),
                AdProperty.AD_SKIP_URLS.bind(asList("skip1", "skip2")))
                .merge(companionDisplayProperties());
    }

    public static PropertySet companionDisplayProperties() {
        return PropertySet.from(
                AdProperty.COMPANION_URN.bind("ad:visual:123"),
                AdProperty.ARTWORK.bind(Uri.parse("http://ad.artwork.url")),
                AdProperty.CLICK_THROUGH_LINK.bind(Uri.parse("http://ad.click.through.url")),
                AdProperty.AD_CLICKTHROUGH_URLS.bind(asList("click1", "click2")),
                AdProperty.AD_COMPANION_DISPLAY_IMPRESSION_URLS.bind(asList("visual1", "visual2")),
                AdProperty.DEFAULT_TEXT_COLOR.bind("#000000"),
                AdProperty.DEFAULT_BACKGROUND_COLOR.bind("#FFFFF"),
                AdProperty.PRESSED_TEXT_COLOR.bind("#111111"),
                AdProperty.PRESSED_BACKGROUND_COLOR.bind("#222222"),
                AdProperty.FOCUSED_TEXT_COLOR.bind("#333333"),
                AdProperty.FOCUSED_BACKGROUND_COLOR.bind("#444444")
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
                LeaveBehindProperty.TRACKING_IMPRESSION_URLS.bind(asList("leaveBehindTrackingImpressionUrl1", "leaveBehindTrackingImpressionUrl2")),
                LeaveBehindProperty.TRACKING_CLICK_URLS.bind(asList("leaveBehindTrackingClickTroughUrl1", "leaveBehindTrackingClickTroughUrl2"))
        );
    }

    public static PropertySet interstitialForPlayer() {
        return PropertySet.from(
                InterstitialProperty.INTERSTITIAL_URN.bind("adswizz:ads:1105"),
                InterstitialProperty.IMAGE_URL.bind("https://va.sndcdn.com/mlb/sqsp-example-leave-behind.jpg"),
                InterstitialProperty.CLICK_THROUGH_URL.bind(Uri.parse("http://squarespace.com")),
                InterstitialProperty.TRACKING_IMPRESSION_URLS.bind(asList("https://promoted.soundcloud.com/impression?adData=instance%3Asoundcloud%3Bad_id%3A1105%3Bview_key%3A1410853892331806%3Bzone_id%3A56&loc=&listenerId=5284047f4ffb4e04824a2fd1d1f0cd62&sessionId=67fa476869b956676b5bae2866c377a9&ip=%3A%3Affff%3A80.82.202.196&OAGEO=ZGUlN0MxNiU3Q2JlcmxpbiU3QzEwMTE1JTdDNTIuNTMxOTk3NjgwNjY0MDYlN0MxMy4zOTIxOTY2NTUyNzM0MzglN0MlN0MlN0MlN0MlM0ElM0FmZmZmJTNBODAuODIuMjAyLjE5NiU3Q3RoZSt1bmJlbGlldmFibGUrbWFjaGluZStjb21wYW55K2dtYmg=&user_agent=SoundCloud-Android%2F14.09.02+%28Android+4.3%3B+Genymotion+Sony+Xperia+Z+-+4.3+-+API+18+-+1080x1920%29&cbs=681405")),
                InterstitialProperty.TRACKING_CLICK_URLS.bind(asList("https://promoted.soundcloud.com/track?reqType=SCAdClicked&protocolVersion=2.0&adId=1105&zoneId=56&cb=dfd1b6e0c90745e9934f9d35b174ff30")),
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
                TrackProperty.PERMALINK_URL.bind("http://permalink.url"),
                TrackProperty.DURATION.bind(10L),
                TrackProperty.PLAY_COUNT.bind(4),
                TrackProperty.LIKES_COUNT.bind(2),
                LikeProperty.CREATED_AT.bind(new Date()),
                TrackProperty.IS_PRIVATE.bind(false));
    }

    public static PropertySet expectedPromotedTrack() {
        return basePromotedTrack()
                .put(PromotedItemProperty.PROMOTER_URN, Optional.of(Urn.forUser(193L)))
                .put(PromotedItemProperty.PROMOTER_NAME, Optional.of("SoundCloud"));
    }

    public static PropertySet expectedPromotedTrackWithoutPromoter() {
        return basePromotedTrack()
                .put(PromotedItemProperty.PROMOTER_URN, Optional.<Urn>absent())
                .put(PromotedItemProperty.PROMOTER_NAME, Optional.<String>absent());
    }

    private static PropertySet basePromotedTrack() {
        return expectedTrackForListItem(Urn.forTrack(12345L))
                .put(PromotedItemProperty.AD_URN, "ad:urn:123")
                .put(PromotedItemProperty.CREATED_AT, new Date(Long.MAX_VALUE))
                .put(PromotedItemProperty.TRACK_CLICKED_URLS, asList("promoted1", "promoted2"))
                .put(PromotedItemProperty.TRACK_IMPRESSION_URLS, asList("promoted3", "promoted4"))
                .put(PromotedItemProperty.TRACK_PLAYED_URLS, asList("promoted5", "promoted6"))
                .put(PromotedItemProperty.PROMOTER_CLICKED_URLS, asList("promoted7", "promoted8"));
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
                PlaylistProperty.IS_PRIVATE.bind(false),
                PostProperty.CREATED_AT.bind(new Date()),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"));
    }

    public static PropertySet expectedFollowingForFollowingsScreen(long position) {
        return PropertySet.from(
                UserProperty.URN.bind(Urn.forUser(123L)),
                UserProperty.USERNAME.bind("avieciie"),
                UserProperty.COUNTRY.bind("country"),
                UserProperty.FOLLOWERS_COUNT.bind(2),
                UserAssociationProperty.POSITION.bind(position),
                UserProperty.IS_FOLLOWED_BY_ME.bind(true));
    }

    public static PropertySet expectedPostedPlaylistForPostsScreen() {
        return expectedPostedPlaylistsForPostedPlaylistsScreen();
    }

    public static PropertySet expectedPostedTrackForPostsScreen() {
        return PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TITLE.bind("squirlex galore part 2"),
                PlaylistProperty.CREATOR_NAME.bind("avieciie"),
                PlaylistProperty.DURATION.bind(123456L),
                PlaylistProperty.LIKES_COUNT.bind(2),
                PlaylistProperty.IS_PRIVATE.bind(false),
                PostProperty.CREATED_AT.bind(new Date()));
    }

    private static PropertySet basePromotedPlaylist() {
        return expectedPostedPlaylistsForPostedPlaylistsScreen()
                .put(PromotedItemProperty.AD_URN, "ad:urn:123")
                .put(PromotedItemProperty.CREATED_AT, new Date(Long.MAX_VALUE))
                .put(PromotedItemProperty.TRACK_CLICKED_URLS, asList("promoted1", "promoted2"))
                .put(PromotedItemProperty.TRACK_IMPRESSION_URLS, asList("promoted3", "promoted4"))
                .put(PromotedItemProperty.TRACK_PLAYED_URLS, asList("promoted5", "promoted6"))
                .put(PromotedItemProperty.PROMOTER_CLICKED_URLS, asList("promoted7", "promoted8"));
    }

    public static PropertySet expectedPromotedPlaylist() {
        return basePromotedPlaylist()
                .put(PromotedItemProperty.PROMOTER_URN, Optional.of(Urn.forUser(193L)))
                .put(PromotedItemProperty.PROMOTER_NAME, Optional.of("SoundCloud"));
    }

    public static PropertySet expectedPromotedPlaylistWithoutPromoter() {
        return basePromotedPlaylist()
                .put(PromotedItemProperty.PROMOTER_URN, Optional.<Urn>absent())
                .put(PromotedItemProperty.PROMOTER_NAME, Optional.<String>absent());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Analytics / Tracking
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static PropertySet expectedTrackForAnalytics(Urn trackUrn, Urn creatorUrn, String policy, long duration) {
        return PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                TrackProperty.CREATOR_URN.bind(creatorUrn),
                TrackProperty.POLICY.bind(policy),
                PlayableProperty.DURATION.bind(duration)
        );
    }

    public static PropertySet expectedTrackForAnalytics(Urn trackUrn, Urn creatorUrn) {
        return expectedTrackForAnalytics(trackUrn, creatorUrn, "allow", 1000);
    }

    public static PropertySet fromApiTrack() {
        return fromApiTrack(ModelFixtures.create(ApiTrack.class));
    }

    public static PropertySet fromApiTrack(ApiTrack apiTrack) {
        return fromApiTrack(apiTrack, false, false, false);
    }

    public static PropertySet fromApiTrack(ApiTrack apiTrack, boolean isPrivate, boolean isLiked, boolean isReposted) {
        return PropertySet.from(
                TrackProperty.URN.bind(apiTrack.getUrn()),
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

    public static PropertySet activityTrackLike() {
        return basicActivity()
                .put(ActivityProperty.KIND, ActivityKind.TRACK_LIKE)
                .put(ActivityProperty.PLAYABLE_TITLE, "sounds of ze forzz");
    }

    public static PropertySet activityTrackRepost() {
        return basicActivity()
                .put(ActivityProperty.KIND, ActivityKind.TRACK_REPOST)
                .put(ActivityProperty.PLAYABLE_TITLE, "sounds of ze forzz");
    }

    public static PropertySet activityPlaylistLike() {
        return basicActivity()
                .put(ActivityProperty.KIND, ActivityKind.PLAYLIST_LIKE)
                .put(ActivityProperty.PLAYABLE_TITLE, "sounds of ze forzz");
    }

    public static PropertySet activityPlaylistRepost() {
        return basicActivity()
                .put(ActivityProperty.KIND, ActivityKind.PLAYLIST_REPOST)
                .put(ActivityProperty.PLAYABLE_TITLE, "sounds of ze forzz");
    }

    public static PropertySet activityUserFollow() {
        return basicActivity().put(ActivityProperty.KIND, ActivityKind.USER_FOLLOW);
    }

    public static PropertySet activityTrackComment() {
        return basicActivity()
                .put(ActivityProperty.KIND, ActivityKind.TRACK_COMMENT)
                .put(ActivityProperty.COMMENTED_TRACK_URN, Urn.forTrack(123))
                .put(ActivityProperty.PLAYABLE_TITLE, "sounds of ze forzz");
    }

    private static PropertySet basicActivity() {
        return PropertySet.from(
                ActivityProperty.DATE.bind(new Date()),
                ActivityProperty.USER_NAME.bind("forss"),
                ActivityProperty.USER_URN.bind(Urn.forUser(2L))
        );
    }

    public static PropertySet midTierTrack() {
        return fromApiTrack(ModelFixtures.create(ApiTrack.class)).put(TrackProperty.SUB_MID_TIER, true);
    }
}
