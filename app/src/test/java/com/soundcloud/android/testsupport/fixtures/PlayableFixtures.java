package com.soundcloud.android.testsupport.fixtures;

import static java.util.Arrays.asList;

import com.soundcloud.android.activities.ActivityItem;
import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.profile.Following;
import com.soundcloud.android.profile.LastPostedTrack;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Date;

public abstract class PlayableFixtures {

    public static final String MONETIZATION_MODEL = "monetization-model";

    public static ApiUser user() {
        return ModelFixtures.apiUser();
    }

    public static TrackItem.Builder baseTrackBuilder() {
        final Track track = ModelFixtures.baseTrackBuilder().build();
        return TrackItem.from(track).toBuilder();
    }

    public static TrackItem expectedTrackForWidget() {
        return TrackItem.from(ModelFixtures.baseTrackBuilder().imageUrlTemplate(Optional.of("https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg")).build());
    }

    public static TrackItem upsellableTrackForPlayer() {
        final Track.Builder builder = expectedTrackBuilderForPlayer();
        builder.snipped(true);
        builder.subHighTier(true);
        return TrackItem.from(builder.build());
    }

    public static TrackItem downloadedTrack() {
        return baseTrackBuilder().offlineState(OfflineState.DOWNLOADED).build();
    }

    public static TrackItem removedTrack() {
        return baseTrackBuilder().offlineState(OfflineState.NOT_OFFLINE).build();
    }

    public static TrackItem expectedTrackForPlayer() {
        return expectedTrackItemBuilderForPlayer().build();
    }

    public static TrackItem.Builder expectedTrackItemBuilderForPlayer() {
        final Track.Builder trackBuilder = expectedTrackBuilderForPlayer().policy(Strings.EMPTY);
        return TrackItem.from(trackBuilder.build()).toBuilder();
    }

    public static Track.Builder expectedTrackBuilderForPlayer() {
        return ModelFixtures.baseTrackBuilder().imageUrlTemplate(Optional.of("https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg"))
                            .playCount(1)
                            .commentsCount(1)
                            .commentable(true)
                            .title("dubstep anthem")
                            .creatorName("squirlex")
                            .monetizationModel(MONETIZATION_MODEL)
                            .creatorUrn(Urn.forUser(456L))
                            .snippetDuration(20000L)
                            .fullDuration(30000L)
                            .snipped(false)
                            .userLike(true)
                            .likesCount(1)
                            .repostsCount(1)
                            .permalinkUrl("http://permalink.url")
                            .isPrivate(false)
                            .userRepost(false)
                            .createdAt(new Date());
    }

    public static TrackItem expectedPrivateTrackForPlayer() {
        final Track.Builder builder = ModelFixtures.baseTrackBuilder();
        return TrackItem.from(builder.urn(Urn.forTrack(123L))
                                     .isPrivate(true)
                                     .title("dubstep anthem")
                                     .creatorName("squirlex")
                                     .permalinkUrl("http://permalink.url")
                                     .userRepost(false)
                                     .build());
    }

    public static TrackItem expectedTrackForListItem(Urn urn) {
        final Track.Builder builder = ModelFixtures.baseTrackBuilder().urn(urn)
                                                   .title("Title " + urn)
                                                   .creatorName("Creator " + urn)
                                                   .permalinkUrl("http://permalink.url")
                                                   .snippetDuration(20000L)
                                                   .fullDuration(30000L)
                                                   .snipped(true)
                                                   .playCount(4)
                                                   .likesCount(2)
                                                   .createdAt(new Date())
                                                   .isPrivate(false);
        return TrackItem.from(builder.build());
    }

    public static TrackItem expectedPromotedTrack() {
        return expectedPromotedTrackBuilder("AD_URN").build();
    }

    public static TrackItem.Builder expectedPromotedTrackBuilder(String adUrn) {
        return basePromotedTrack().promotedProperties(getPromotedProperties(Urn.forUser(193L), "SoundCloud", adUrn));
    }

    public static TrackItem.Builder expectedPromotedTrackBuilder(Urn urn, String adUrn) {
        return basePromotedTrack(urn).promotedProperties(getPromotedProperties(Urn.forUser(193L), "SoundCloud", adUrn));
    }

    public static TrackItem expectedPromotedTrackWithoutPromoter() {
        return basePromotedTrack().build();
    }

    private static TrackItem.Builder basePromotedTrack() {
        final Urn urn = Urn.forTrack(12345L);
        return basePromotedTrack(urn);
    }

    private static TrackItem.Builder basePromotedTrack(Urn urn) {
        final Track track = ModelFixtures.baseTrackBuilder()
                                         .urn(urn)
                                         .title("Title " + urn)
                                         .creatorName("Creator " + urn)
                                         .permalinkUrl("http://permalink.url")
                                         .snippetDuration(20000L)
                                         .fullDuration(30000L)
                                         .snipped(true)
                                         .playCount(4)
                                         .likesCount(2)
                                         .createdAt(new Date())
                                         .isPrivate(false).build();
        return TrackItem.from(track)
                        .toBuilder()
                        .promotedProperties(getPromotedProperties());
    }

    private static PromotedProperties getPromotedProperties() {
        return PromotedProperties.create("ad:urn:123",
                                         asList("promoted1", "promoted2"),
                                         asList("promoted3", "promoted4"),
                                         asList("promoted5", "promoted6"),
                                         asList("promoted7", "promoted8"),
                                         Optional.absent(),
                                         Optional.absent());
    }

    private static PromotedProperties getPromotedProperties(Urn promoterUrn, String promoterName, String adUrn) {
        return PromotedProperties.create(adUrn,
                                         asList("promoted1", "promoted2"),
                                         asList("promoted3", "promoted4"),
                                         asList("promoted5", "promoted6"),
                                         asList("promoted7", "promoted8"),
                                         Optional.of(promoterUrn),
                                         Optional.of(promoterName));
    }

    public static PlaylistItem expectedLikedPlaylistForPlaylistsScreen() {
        return PlaylistItem.builder()
                           .getUrn(Urn.forPlaylist(123L))
                           .title("squirlex galore")
                           .creatorName("avieciie")
                           .trackCount(4)
                           .likesCount(2)
                           .getCreatedAt(new Date())
                           .isPrivate(false)
                           .build();
    }

    public static PlaylistItem expectedPostedPlaylistsForPostedPlaylistsScreen() {
        return postedPlaylistForPostedPlaylistScreen(Urn.forPlaylist(123L)).build();
    }

    private static PlaylistItem.Builder postedPlaylistForPostedPlaylistScreen(Urn playlistUrn) {
        return PlaylistItem.builder()
                           .getUrn(playlistUrn)
                           .title("squirlex galore")
                           .creatorName("avieciie")
                           .trackCount(4)
                           .likesCount(2)
                           .getCreatedAt(new Date())
                           .isPrivate(false)
                           .permalinkUrl("http://permalink.url");
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

    private static PlaylistItem.Builder basePromotedPlaylist(PromotedProperties promotedProperties) {
        return PlaylistItem.builder()
                           .getUrn(Urn.forPlaylist(123L))
                           .title("squirlex galore")
                           .creatorName("avieciie")
                           .trackCount(4)
                           .likesCount(2)
                           .getCreatedAt(new Date())
                           .isPrivate(false)
                           .permalinkUrl("http://permalink.url")
                           .promotedProperties(promotedProperties);
    }

    public static PlaylistItem expectedPromotedPlaylist() {
        return basePromotedPlaylist(getPromotedProperties(Urn.forUser(193L), "SoundCloud", "ad:urn:123")).build();
    }

    public static PlaylistItem expectedPromotedPlaylistWithoutPromoter() {
        return basePromotedPlaylist(getPromotedProperties()).build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Analytics / Tracking
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static TrackItem expectedTrackForAnalytics(Urn trackUrn, Urn creatorUrn, String policy, long duration) {
        final Track track = ModelFixtures.baseTrackBuilder().urn(trackUrn)
                                         .creatorUrn(creatorUrn)
                                         .policy(policy)
                                         .monetizationModel(MONETIZATION_MODEL)
                                         .snippetDuration(duration)
                                         .fullDuration(duration)
                                         .snipped(false)
                                         .build();
        return TrackItem.from(track);
    }

    public static TrackItem expectedTrackForAnalytics(Urn trackUrn, Urn creatorUrn) {
        return expectedTrackForAnalytics(trackUrn, creatorUrn, "ALLOW", 1000);
    }

    public static TrackItem fromApiTrack() {
        return fromApiTrack(ModelFixtures.create(ApiTrack.class));
    }

    public static TrackItem fromApiTrack(ApiTrack apiTrack) {
        return builderFromApiTrack(apiTrack).build();
    }

    private static TrackItem.Builder builderFromApiTrack(ApiTrack apiTrack) {
        return builderFromApiTrack(apiTrack, false, false, false);
    }

    private static TrackItem.Builder builderFromApiTrack(ApiTrack apiTrack, boolean isPrivate, boolean isLiked, boolean isReposted) {
        final Track.Builder builder = Track.builder(Track.from(apiTrack));
        builder.isPrivate(isPrivate);
        builder.userLike(isLiked);
        builder.userRepost(isReposted);
        return TrackItem.from(builder.build()).toBuilder();
    }

    public static PlaylistItem fromApiPlaylist() {
        return fromApiPlaylist(ModelFixtures.create(ApiPlaylist.class), false, false, false);
    }

    public static PlaylistItem fromApiPlaylist(ApiPlaylist apiPlaylist,
                                               boolean isLiked,
                                               boolean isReposted,
                                               boolean markedForOffline) {
        final PlaylistItem.Builder playlistItem = PlaylistItem.from(apiPlaylist).toBuilder();
        playlistItem.isUserLike(isLiked);
        playlistItem.isRepost(isReposted);
        playlistItem.isMarkedForOffline(Optional.of(markedForOffline));
        return playlistItem.build();
    }

    public static StreamEntity timelineItem(Date createdAt) {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final Urn urn = apiTrack.getUrn();
        return StreamEntity.builder(urn, createdAt, Optional.absent(), Optional.absent(), apiTrack.getUser().getImageUrlTemplate()).build();
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
        final Track.Builder builder = highTierTrackBuilder();
        return TrackItem.from(builder.build());
    }

    public static Track.Builder highTierTrackBuilder() {
        return ModelFixtures.trackBuilder().subHighTier(true);
    }

    public static TrackItem upsellableTrack() {
        return TrackItem.from(upsellableTrackBuilder().build());
    }

    public static Track.Builder upsellableTrackBuilder() {
        final Track.Builder builder = highTierTrackBuilder();
        builder.snipped(true);
        return builder;
    }
}
