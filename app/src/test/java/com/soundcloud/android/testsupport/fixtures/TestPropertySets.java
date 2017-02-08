package com.soundcloud.android.testsupport.fixtures;

import static java.util.Arrays.asList;

import com.soundcloud.android.activities.ActivityItem;
import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.profile.Following;
import com.soundcloud.android.profile.LastPostedTrack;
import com.soundcloud.android.stream.StreamPlayable;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public abstract class TestPropertySets {

    public static final String MONETIZATION_MODEL = "monetization-model";

    public static ApiUser user() {
        return ModelFixtures.create(ApiUser.class);
    }

    public static PropertySet mandatoryTrackProperties() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.TITLE.bind("someone's favorite song"),
                PlayableProperty.CREATOR_NAME.bind("someone's favorite band"),
                PlayableProperty.CREATOR_URN.bind(Urn.forUser(123L)),
                TrackProperty.SNIPPET_DURATION.bind(10L),
                TrackProperty.FULL_DURATION.bind(1000L),
                PlayableProperty.IS_USER_LIKE.bind(false),
                TrackProperty.IS_USER_REPOST.bind(false),
                TrackProperty.LIKES_COUNT.bind(0),
                TrackProperty.PERMALINK_URL.bind("http://soundcloud.com/artist/track_permalink")
        );
    }

    public static TrackItem expectedTrackForWidget() {
        return TestPropertySets.trackWith(PropertySet.from(
                TrackProperty.IMAGE_URL_TEMPLATE.bind(Optional.of("https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg"))
        ));
    }

    public static TrackItem upsellableTrackForPlayer() {
        final TrackItem trackItem = expectedTrackForPlayer();
        trackItem.setSnipped(true);
        trackItem.setSubHighTier(true);
        return trackItem;
    }

    public static TrackItem expectedTrackForPlayer() {
        return TestPropertySets.trackWith(PropertySet.from(
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
        ));
    }

    public static TrackItem expectedPrivateTrackForPlayer() {
        return trackWith(PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.IS_PRIVATE.bind(true),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind(""),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_USER_REPOST.bind(true)));
    }

    public static TrackItem expectedTrackForListItem(Urn urn) {
        return trackWith(PropertySet.from(
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
                TrackProperty.IS_PRIVATE.bind(false)));
    }

    public static PromotedTrackItem expectedPromotedTrack() {
        final PropertySet properties = basePromotedTrack()
                .put(PromotedItemProperty.PROMOTER_URN, Optional.of(Urn.forUser(193L)))
                .put(PromotedItemProperty.PROMOTER_NAME, Optional.of("SoundCloud"));
        return PromotedTrackItem.from(mandatoryTrackProperties().merge(properties));
    }

    public static PromotedTrackItem expectedPromotedTrackWithoutPromoter() {
        final PropertySet properties = basePromotedTrack()
                .put(PromotedItemProperty.PROMOTER_URN, Optional.absent())
                .put(PromotedItemProperty.PROMOTER_NAME, Optional.absent());
        return PromotedTrackItem.from(mandatoryTrackProperties().merge(properties));
    }

    public static TrackItem trackWith(PropertySet properties) {
        return TrackItem.from(mandatoryTrackProperties().merge(properties));
    }

    private static PropertySet basePromotedTrack() {
        final Urn urn = Urn.forTrack(12345L);
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
                TrackProperty.IS_PRIVATE.bind(false),
                PromotedItemProperty.AD_URN.bind("ad:urn:123"),
                PromotedItemProperty.CREATED_AT.bind(new Date(Long.MAX_VALUE)),
                PromotedItemProperty.TRACK_CLICKED_URLS.bind(asList("promoted1", "promoted2")),
                PromotedItemProperty.TRACK_IMPRESSION_URLS.bind(asList("promoted3", "promoted4")),
                PromotedItemProperty.TRACK_PLAYED_URLS.bind(asList("promoted5", "promoted6")),
                PromotedItemProperty.PROMOTER_CLICKED_URLS.bind(asList("promoted7", "promoted8")));
    }


    public static PlaylistItem expectedLikedPlaylistForPlaylistsScreen() {
        return PlaylistItem.from(PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forPlaylist(123L)),
                PlaylistProperty.TITLE.bind("squirlex galore"),
                PlaylistProperty.CREATOR_NAME.bind("avieciie"),
                PlaylistProperty.TRACK_COUNT.bind(4),
                PlaylistProperty.LIKES_COUNT.bind(2),
                LikeProperty.CREATED_AT.bind(new Date()),
                PlaylistProperty.IS_PRIVATE.bind(false)));
    }

    public static PlaylistItem expectedPostedPlaylistsForPostedPlaylistsScreen() {
        return PlaylistItem.from(postedPlaylistForPostedPlaylistScreen(Urn.forPlaylist(123L)));
    }

    private static PropertySet postedPlaylistForPostedPlaylistScreen(Urn playlistUrn) {
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

    public static Following expectedFollowingForFollowingsScreen(long position) {
        final Urn urn = Urn.forUser(123L);
        return Following.from(
                UserItem.create(urn, "avieciie", Optional.absent(), Optional.of("country"), 2, true),
                UserAssociation.create(urn, position, -1, Optional.absent(), Optional.absent()));
    }

    public static PlaylistItem expectedPostedPlaylistForPostsScreen() {
        return expectedPostedPlaylistsForPostedPlaylistsScreen();
    }

    public static LastPostedTrack expectedLastPostedTrackForPostsScreen() {
        return LastPostedTrack.create(Urn.forTrack(123L), new Date(), "http://permalink.url");
    }

    private static PropertySet basePromotedPlaylist() {
        return postedPlaylistForPostedPlaylistScreen(Urn.forPlaylist(123L))
                .put(PromotedItemProperty.AD_URN, "ad:urn:123")
                .put(PromotedItemProperty.CREATED_AT, new Date(Long.MAX_VALUE))
                .put(PromotedItemProperty.TRACK_CLICKED_URLS, asList("promoted1", "promoted2"))
                .put(PromotedItemProperty.TRACK_IMPRESSION_URLS, asList("promoted3", "promoted4"))
                .put(PromotedItemProperty.TRACK_PLAYED_URLS, asList("promoted5", "promoted6"))
                .put(PromotedItemProperty.PROMOTER_CLICKED_URLS, asList("promoted7", "promoted8"));
    }

    public static PromotedPlaylistItem expectedPromotedPlaylist() {
        return PromotedPlaylistItem.from(basePromotedPlaylist()
                                                 .put(PromotedItemProperty.PROMOTER_URN, Optional.of(Urn.forUser(193L)))
                                                 .put(PromotedItemProperty.PROMOTER_NAME, Optional.of("SoundCloud")));
    }

    public static PromotedPlaylistItem expectedPromotedPlaylistWithoutPromoter() {
        return PromotedPlaylistItem.from(basePromotedPlaylist()
                                                 .put(PromotedItemProperty.PROMOTER_URN, Optional.<Urn>absent())
                                                 .put(PromotedItemProperty.PROMOTER_NAME, Optional.<String>absent()));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Analytics / Tracking
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static TrackItem expectedTrackForAnalytics(Urn trackUrn, Urn creatorUrn, String policy, long duration) {
        return trackWith(PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                TrackProperty.CREATOR_URN.bind(creatorUrn),
                TrackProperty.POLICY.bind(policy),
                TrackProperty.MONETIZATION_MODEL.bind(MONETIZATION_MODEL),
                TrackProperty.SNIPPET_DURATION.bind(duration),
                TrackProperty.FULL_DURATION.bind(duration),
                TrackProperty.SNIPPED.bind(false)
        ));
    }

    public static TrackItem expectedTrackForAnalytics(Urn trackUrn, Urn creatorUrn) {
        return expectedTrackForAnalytics(trackUrn, creatorUrn, "ALLOW", 1000);
    }

    public static TrackItem fromApiTrack() {
        return fromApiTrack(ModelFixtures.create(ApiTrack.class));
    }

    public static TrackItem fromApiTrack(ApiTrack apiTrack) {
        return fromApiTrack(apiTrack, false, false, false);
    }

    public static TrackItem fromApiTrack(ApiTrack apiTrack, boolean isPrivate, boolean isLiked, boolean isReposted) {
        final TrackItem trackItem = TrackItem.from(apiTrack);
        trackItem.setPrivate(isPrivate);
        trackItem.setLikedByCurrentUser(isLiked);
        trackItem.setRepost(isReposted);
        return trackItem;
    }

    public static PlaylistItem fromApiPlaylist() {
        return fromApiPlaylist(ModelFixtures.create(ApiPlaylist.class), false, false, false);
    }

    public static PlaylistItem fromApiPlaylist(ApiPlaylist apiPlaylist,
                                               boolean isLiked,
                                               boolean isReposted,
                                               boolean markedForOffline) {
        final PlaylistItem playlistItem = PlaylistItem.from(apiPlaylist);
        playlistItem.setLikedByCurrentUser(isLiked);
        playlistItem.setRepost(isReposted);
        playlistItem.setMarkedForOffline(markedForOffline);
        return playlistItem;
    }

    public static StreamPlayable timelineItem(Date createdAt) {
        final PropertySet properties = PropertySet.from(
                PlayableProperty.URN.bind(ModelFixtures.create(ApiTrack.class).getUrn()),
                PlayableProperty.CREATED_AT.bind(createdAt));
        return StreamPlayable.createFromPropertySet(createdAt, mandatoryTrackProperties().merge(properties));
    }

    public static ActivityItem activityTrackLike(Date createdAt) {
        return basicActivity().kind(ActivityKind.TRACK_LIKE).createdAt(createdAt).build();
    }

    public static ActivityItem activityTrackLike() {
        return basicActivity().kind(ActivityKind.TRACK_LIKE).build();
    }

    public static ActivityItem activityTrackRepost() {
        return basicActivity().kind(ActivityKind.TRACK_REPOST).build();
    }

    public static ActivityItem activityPlaylistLike() {
        return basicActivity().kind(ActivityKind.PLAYLIST_LIKE).build();
    }

    public static ActivityItem activityPlaylistRepost() {
        return basicActivity().kind(ActivityKind.PLAYLIST_REPOST).build();
    }

    public static ActivityItem activityUserFollow() {
        return basicActivity().kind(ActivityKind.USER_FOLLOW).build();
    }

    public static ActivityItem activityTrackComment(Urn trackUrn) {
        return basicActivity()
                .kind(ActivityKind.TRACK_COMMENT)
                .commentedTrackUrn(Optional.of(trackUrn))
                .playableTitle("sounds of ze forzz")
                .build();
    }

    private static ActivityItem.Builder basicActivity() {
        return ActivityItem.builder()
                           .createdAt(new Date())
                           .userName("forss")
                           .urn(Urn.forUser(2L))
                           .playableTitle("sounds of ze forzz");

    }

    public static TrackItem highTierTrack() {
        final TrackItem trackItem = fromApiTrack(ModelFixtures.create(ApiTrack.class));
        trackItem.setSubHighTier(true);
        return trackItem;
    }

    public static TrackItem upsellableTrack() {
        final TrackItem trackItem = highTierTrack();
        trackItem.setSnipped(true);
        return trackItem;
    }
}
