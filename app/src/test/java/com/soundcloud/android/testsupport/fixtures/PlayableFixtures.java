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
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.profile.Following;
import com.soundcloud.android.profile.LastPostedTrack;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public abstract class PlayableFixtures {

    public static final String MONETIZATION_MODEL = "monetization-model";

    public static ApiUser user() {
        return ModelFixtures.apiUser();
    }

    public static TrackItem.Default.Builder baseTrackBuilder() {
        return TrackItem.builder()
                        .getUrn(Urn.forTrack(123L))
                        .title("someone's favorite song")
                        .creatorName("someone's favorite band")
                        .creatorUrn(Urn.forUser(123L))
                        .snippetDuration(10L)
                        .fullDuration(1000L)
                        .isUserLike(false)
                        .isUserRepost(false)
                        .likesCount(0)
                        .permalinkUrl("http://soundcloud.com/artist/track_permalink");
    }

    public static TrackItem expectedTrackForWidget() {
        return PlayableFixtures.baseTrackBuilder().getImageUrlTemplate(Optional.of("https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg")).build();
    }

    public static TrackItem upsellableTrackForPlayer() {
        final TrackItem.Default.Builder builder = expectedTrackBuilderForPlayer();
        builder.isSnipped(true);
        builder.isSubHighTier(true);
        return builder.build();
    }

    public static TrackItem downloadedTrack() {
        return baseTrackBuilder().offlineState(OfflineState.DOWNLOADED).build();
    }

    public static TrackItem removedTrack() {
        return baseTrackBuilder().offlineState(OfflineState.NOT_OFFLINE).build();
    }

    public static TrackItem.Default expectedTrackForPlayer() {
        return expectedTrackBuilderForPlayer().build();
    }

    public static TrackItem.Default.Builder expectedTrackBuilderForPlayer() {
        final TrackItem.Default.Builder builder = PlayableFixtures.baseTrackBuilder();
        return builder.getImageUrlTemplate(Optional.of("https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg"))
                      .playCount(1)
                      .commentsCount(1)
                      .commentable(true)
                      .title("dubstep anthem")
                      .creatorName("squirlex")
                      .monetizationModel(MONETIZATION_MODEL)
                      .creatorUrn(Urn.forUser(456L))
                      .snippetDuration(20000L)
                      .fullDuration(30000L)
                      .isSnipped(false)
                      .isUserLike(true)
                      .likesCount(1)
                      .repostsCount(1)
                      .permalinkUrl("http://permalink.url")
                      .isPrivate(false)
                      .isUserRepost(false)
                      .getCreatedAt(new Date());
    }

    public static TrackItem expectedPrivateTrackForPlayer() {
        final TrackItem.Default.Builder builder = PlayableFixtures.baseTrackBuilder();
        return builder.getUrn(Urn.forTrack(123L))
                      .isPrivate(true)
                      .title("dubstep anthem")
                      .creatorName("squirlex")
                      .permalinkUrl("http://permalink.url")
                      .isUserRepost(false)
                      .build();
    }

    public static TrackItem expectedTrackForListItem(Urn urn) {
        return baseTrackBuilder().getUrn(urn)
                                 .title("Title " + urn)
                                 .creatorName("Creator " + urn)
                                 .permalinkUrl("http://permalink.url")
                                 .snippetDuration(20000L)
                                 .fullDuration(30000L)
                                 .isSnipped(true)
                                 .playCount(4)
                                 .likesCount(2)
                                 .getCreatedAt(new Date())
                                 .isPrivate(false)
                                 .build();
    }

    public static PromotedTrackItem expectedPromotedTrack() {
        final PromotedTrackItem.Builder builder = expectedPromotedTrackBuilder("AD_URN");
        return builder.build();
    }

    public static PromotedTrackItem.Builder expectedPromotedTrackBuilder(String adUrn) {
        return basePromotedTrack().promotedProperties(getPromotedProperties(Urn.forUser(193L), "SoundCloud", adUrn));
    }

    public static PromotedTrackItem expectedPromotedTrackWithoutPromoter() {
        return basePromotedTrack().build();
    }

    private static PromotedTrackItem.Builder basePromotedTrack() {
        final Urn urn = Urn.forTrack(12345L);
        return PromotedTrackItem.builder(getPromotedProperties())
                                .getUrn(urn)
                                .title("Title " + urn)
                                .creatorName("Creator " + urn)
                                .permalinkUrl("http://permalink.url")
                                .snippetDuration(20000L)
                                .fullDuration(30000L)
                                .isSnipped(true)
                                .playCount(4)
                                .likesCount(2)
                                .getCreatedAt(new Date())
                                .isPrivate(false);
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

    private static PlaylistItem.Default.Builder postedPlaylistForPostedPlaylistScreen(Urn playlistUrn) {
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

    private static PromotedPlaylistItem.Builder basePromotedPlaylist(PromotedProperties promotedProperties) {
        return PromotedPlaylistItem.builder(promotedProperties)
                                   .getUrn(Urn.forPlaylist(123L))
                                   .title("squirlex galore")
                                   .creatorName("avieciie")
                                   .trackCount(4)
                                   .likesCount(2)
                                   .getCreatedAt(new Date())
                                   .isPrivate(false)
                                   .permalinkUrl("http://permalink.url");
    }

    public static PromotedPlaylistItem expectedPromotedPlaylist() {
        return basePromotedPlaylist(getPromotedProperties(Urn.forUser(193L), "SoundCloud", "ad:urn:123")).build();
    }

    public static PromotedPlaylistItem expectedPromotedPlaylistWithoutPromoter() {
        return basePromotedPlaylist(getPromotedProperties()).build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Analytics / Tracking
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static TrackItem expectedTrackForAnalytics(Urn trackUrn, Urn creatorUrn, String policy, long duration) {
        return baseTrackBuilder().getUrn(trackUrn)
                                 .creatorUrn(creatorUrn)
                                 .policy(policy)
                                 .monetizationModel(MONETIZATION_MODEL)
                                 .snippetDuration(duration)
                                 .fullDuration(duration)
                                 .isSnipped(false)
                                 .build();
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

    private static TrackItem.Default.Builder builderFromApiTrack(ApiTrack apiTrack) {
        return builderFromApiTrack(apiTrack, false, false, false);
    }

    private static TrackItem.Default.Builder builderFromApiTrack(ApiTrack apiTrack, boolean isPrivate, boolean isLiked, boolean isReposted) {
        final TrackItem.Default.Builder builder = TrackItem.builder(TrackItem.from(apiTrack));
        builder.isPrivate(isPrivate);
        builder.isUserLike(isLiked);
        builder.isRepost(isReposted);
        return builder;
    }

    public static PlaylistItem fromApiPlaylist() {
        return fromApiPlaylist(ModelFixtures.create(ApiPlaylist.class), false, false, false);
    }

    public static PlaylistItem fromApiPlaylist(ApiPlaylist apiPlaylist,
                                               boolean isLiked,
                                               boolean isReposted,
                                               boolean markedForOffline) {
        final PlaylistItem.Default.Builder playlistItem = PlaylistItem.builder(PlaylistItem.from(apiPlaylist));
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
        final TrackItem.Default.Builder builder = highTierTrackBuilder();
        return builder.build();
    }

    public static TrackItem.Default.Builder highTierTrackBuilder() {
        final TrackItem.Default.Builder builder = builderFromApiTrack(ModelFixtures.create(ApiTrack.class));
        builder.isSubHighTier(true);
        return builder;
    }

    public static TrackItem upsellableTrack() {
        return upsellableTrackBuilder().build();
    }

    public static TrackItem.Default.Builder upsellableTrackBuilder() {
        final TrackItem.Default.Builder builder = highTierTrackBuilder();
        builder.isSnipped(true);
        return builder;
    }
}
