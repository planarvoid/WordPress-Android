package com.soundcloud.android.testsupport.fixtures;

import static java.util.Arrays.asList;

import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserAssociationProperty;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public abstract class TestPropertySets {

    public static final String MONETIZATION_MODEL = "monetization-model";

    public static PropertySet user() {
        return ModelFixtures.create(ApiUser.class).toPropertySet();
    }

    public static PropertySet expectedTrackForWidget() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                TrackProperty.IMAGE_URL_TEMPLATE.bind(Optional.of(
                        "https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg")),
                PlayableProperty.TITLE.bind("someone's favorite song"),
                PlayableProperty.CREATOR_NAME.bind("someone's favorite band"),
                PlayableProperty.CREATOR_URN.bind(Urn.forUser(123L)),
                PlayableProperty.IS_USER_LIKE.bind(false),
                AdProperty.IS_AUDIO_AD.bind(false)
        );
    }

    public static PropertySet upsellableTrackForPlayer() {
        return expectedTrackForPlayer().put(TrackProperty.SNIPPED, true).put(TrackProperty.SUB_HIGH_TIER, true);
    }

    public static PropertySet expectedTrackForPlayer() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                TrackProperty.WAVEFORM_URL.bind("http://waveform.url"),
                TrackProperty.IMAGE_URL_TEMPLATE.bind(Optional.of(
                        "https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg")),
                TrackProperty.PLAY_COUNT.bind(1),
                TrackProperty.COMMENTS_COUNT.bind(1),
                TrackProperty.IS_COMMENTABLE.bind(true),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind("squirlex"),
                TrackProperty.MONETIZATION_MODEL.bind(MONETIZATION_MODEL),
                PlayableProperty.CREATOR_URN.bind(Urn.forUser(456L)),
                TrackProperty.SNIPPET_DURATION.bind(20000L),
                TrackProperty.FULL_DURATION.bind(30000L),
                TrackProperty.SNIPPED.bind(false),
                PlayableProperty.IS_USER_LIKE.bind(true),
                PlayableProperty.LIKES_COUNT.bind(1),
                PlayableProperty.REPOSTS_COUNT.bind(1),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_PRIVATE.bind(false),
                PlayableProperty.IS_USER_REPOST.bind(false),
                PlayableProperty.CREATED_AT.bind(new Date())
        );
    }

    public static PropertySet expectedPrivateTrackForPlayer() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.IS_PRIVATE.bind(true),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind(""),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_USER_REPOST.bind(true));
    }

    public static PropertySet expectedTrackForListItem(Urn urn) {
        return PropertySet.from(
                TrackProperty.URN.bind(urn),
                TrackProperty.TITLE.bind("Title " + urn),
                TrackProperty.CREATOR_NAME.bind("Creator " + urn),
                TrackProperty.PERMALINK_URL.bind("http://permalink.url"),
                TrackProperty.SNIPPET_DURATION.bind(20000L),
                TrackProperty.FULL_DURATION.bind(30000L),
                TrackProperty.SNIPPED.bind(true),
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
        return expectedLikedTrackForLikesScreenWithDate(new Date());
    }

    public static PropertySet expectedLikedTrackForLikesScreenWithDate(Date value) {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                TrackProperty.TITLE.bind("squirlex galore"),
                TrackProperty.CREATOR_NAME.bind("avieciie"),
                TrackProperty.SNIPPET_DURATION.bind(10000L),
                TrackProperty.FULL_DURATION.bind(20000L),
                TrackProperty.PLAY_COUNT.bind(4),
                TrackProperty.LIKES_COUNT.bind(2),
                LikeProperty.CREATED_AT.bind(value),
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
        return postedPlaylistForPostedPlaylistScreen(Urn.forPlaylist(123L));
    }

    public static PropertySet postedPlaylistForPostedPlaylistScreen(Urn playlistUrn) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlistUrn),
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
                PlayableProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.TITLE.bind("squirlex galore part 2"),
                PlayableProperty.CREATOR_NAME.bind("avieciie"),
                TrackProperty.SNIPPET_DURATION.bind(123456L),
                TrackProperty.FULL_DURATION.bind(123456L),
                PlayableProperty.LIKES_COUNT.bind(2),
                PlayableProperty.IS_PRIVATE.bind(false),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
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
                TrackProperty.MONETIZATION_MODEL.bind(MONETIZATION_MODEL),
                TrackProperty.SNIPPET_DURATION.bind(duration),
                TrackProperty.FULL_DURATION.bind(duration),
                TrackProperty.SNIPPED.bind(false)
        );
    }

    public static PropertySet expectedTrackForAnalytics(Urn trackUrn, Urn creatorUrn) {
        return expectedTrackForAnalytics(trackUrn, creatorUrn, "ALLOW", 1000);
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
                TrackProperty.SNIPPET_DURATION.bind(apiTrack.getSnippetDuration()),
                TrackProperty.FULL_DURATION.bind(apiTrack.getFullDuration()),
                PlayableProperty.CREATOR_NAME.bind(apiTrack.getUser().getUsername()),
                PlayableProperty.CREATOR_URN.bind(apiTrack.getUser().getUrn()),
                TrackProperty.WAVEFORM_URL.bind(apiTrack.getWaveformUrl()),
                EntityProperty.IMAGE_URL_TEMPLATE.bind(apiTrack.getImageUrlTemplate()),
                TrackProperty.PLAY_COUNT.bind(apiTrack.getStats().getPlaybackCount()),
                TrackProperty.COMMENTS_COUNT.bind(apiTrack.getStats().getCommentsCount()),
                TrackProperty.IS_COMMENTABLE.bind(apiTrack.isCommentable()),
                PlayableProperty.LIKES_COUNT.bind(apiTrack.getStats().getLikesCount()),
                PlayableProperty.REPOSTS_COUNT.bind(apiTrack.getStats().getRepostsCount()),
                TrackProperty.MONETIZABLE.bind(apiTrack.isMonetizable()),
                TrackProperty.POLICY.bind(apiTrack.getPolicy()),
                TrackProperty.BLOCKED.bind(apiTrack.isBlocked()),
                TrackProperty.SNIPPED.bind(apiTrack.isSnipped()),
                TrackProperty.SUB_HIGH_TIER.bind(apiTrack.isSubHighTier().get()),
                TrackProperty.MONETIZATION_MODEL.bind(apiTrack.getMonetizationModel().get()),
                PlayableProperty.IS_USER_LIKE.bind(isLiked),
                PlayableProperty.PERMALINK_URL.bind(apiTrack.getPermalinkUrl()),
                PlayableProperty.IS_PRIVATE.bind(isPrivate),
                PlayableProperty.CREATED_AT.bind(apiTrack.getCreatedAt()),
                PlayableProperty.IS_USER_REPOST.bind(isReposted),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.NOT_OFFLINE));
    }

    public static PropertySet likedEntityChangeSet(Urn targetUrn, int likesCount) {
        return PropertySet.from(
                PlayableProperty.URN.bind(targetUrn),
                PlayableProperty.LIKES_COUNT.bind(likesCount),
                PlayableProperty.IS_USER_LIKE.bind(true)
        );
    }

    public static PropertySet unlikedEntityChangeSet(Urn targetUrn, int likesCount) {
        return PropertySet.from(
                PlayableProperty.URN.bind(targetUrn),
                PlayableProperty.LIKES_COUNT.bind(likesCount),
                PlayableProperty.IS_USER_LIKE.bind(false)
        );
    }


    public static PropertySet fromApiPlaylist() {
        return fromApiPlaylist(ModelFixtures.create(ApiPlaylist.class), false, false, false, false);
    }

    public static PropertySet fromApiPlaylist(ApiPlaylist apiPlaylist,
                                              boolean isLiked,
                                              boolean isReposted,
                                              boolean markedForOffline,
                                              boolean isPosted) {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forPlaylist(apiPlaylist.getId())),
                PlayableProperty.TITLE.bind(apiPlaylist.getTitle()),
                EntityProperty.IMAGE_URL_TEMPLATE.bind(apiPlaylist.getImageUrlTemplate()),
                PlaylistProperty.PLAYLIST_DURATION.bind(apiPlaylist.getDuration()),
                PlayableProperty.CREATOR_NAME.bind(apiPlaylist.getUser().getUsername()),
                PlayableProperty.CREATOR_URN.bind(apiPlaylist.getUser().getUrn()),
                PlayableProperty.LIKES_COUNT.bind(apiPlaylist.getStats().getLikesCount()),
                PlayableProperty.REPOSTS_COUNT.bind(apiPlaylist.getStats().getRepostsCount()),
                PlayableProperty.PERMALINK_URL.bind(apiPlaylist.getPermalinkUrl()),
                PlayableProperty.CREATED_AT.bind(apiPlaylist.getCreatedAt()),
                PlayableProperty.IS_PRIVATE.bind(Sharing.PRIVATE.equals(apiPlaylist.getSharing())),
                PlayableProperty.IS_USER_LIKE.bind(isLiked),
                PlayableProperty.IS_USER_REPOST.bind(isReposted),
                PlaylistProperty.IS_POSTED.bind(isPosted),
                OfflineProperty.IS_MARKED_FOR_OFFLINE.bind(markedForOffline),
                PlaylistProperty.TRACK_COUNT.bind(apiPlaylist.getTrackCount()),
                PlaylistProperty.IS_ALBUM.bind(apiPlaylist.isAlbum()),
                PlaylistProperty.SET_TYPE.bind(apiPlaylist.getSetType()),
                PlaylistProperty.RELEASE_DATE.bind(apiPlaylist.getReleaseDate()));
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

    public static PropertySet highTierTrack() {
        return fromApiTrack(ModelFixtures.create(ApiTrack.class)).put(TrackProperty.SUB_HIGH_TIER, true);
    }

    public static PropertySet upsellableTrack() {
        return highTierTrack().put(TrackProperty.SNIPPED, true);
    }

}
