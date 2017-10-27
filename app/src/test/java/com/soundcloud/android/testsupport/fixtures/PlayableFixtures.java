package com.soundcloud.android.testsupport.fixtures;

import static java.util.Arrays.asList;

import com.soundcloud.android.activities.ActivityItem;
import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.profile.LastPostedTrack;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.RepostedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Date;

public abstract class PlayableFixtures {

    public static final String MONETIZATION_MODEL = "monetization-model";

    public static ApiUser user() {
        return UserFixtures.apiUser();
    }

    public static TrackItem.Builder baseTrackBuilder() {
        final Track track = TrackFixtures.track();
        return ModelFixtures.entityItemCreator().trackItem(track).toBuilder();
    }

    public static TrackItem expectedTrackForWidget() {
        return ModelFixtures.entityItemCreator().trackItem(TrackFixtures.trackBuilder().imageUrlTemplate(Optional.of("https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg")).build());
    }

    public static TrackItem upsellableTrackForPlayer() {
        final Track.Builder builder = expectedTrackBuilderForPlayer();
        builder.snipped(true);
        builder.subHighTier(true);
        return ModelFixtures.entityItemCreator().trackItem(builder.build());
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
        return ModelFixtures.entityItemCreator().trackItem(trackBuilder.build()).toBuilder();
    }

    public static Track.Builder expectedTrackBuilderForPlayer() {
        return TrackFixtures.trackBuilder().imageUrlTemplate(Optional.of("https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg"))
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
        final Track.Builder builder = TrackFixtures.trackBuilder();
        return ModelFixtures.entityItemCreator().trackItem(builder.urn(Urn.forTrack(123L))
                                                                 .isPrivate(true)
                                                                 .title("dubstep anthem")
                                                                 .creatorName("squirlex")
                                                                 .permalinkUrl("http://permalink.url")
                                                                 .userRepost(false)
                                                                 .build());
    }

    public static TrackItem expectedTrackForListItem(Urn urn) {
        final Track.Builder builder = TrackFixtures.trackBuilder().urn(urn)
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
        return ModelFixtures.entityItemCreator().trackItem(builder.build());
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
        final Track track = TrackFixtures.trackBuilder()
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
        return ModelFixtures.entityItemCreator().trackItem(track)
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
        final Playlist playlist = PlaylistFixtures.playlistBuilder()
                                                  .urn(Urn.forPlaylist(123L))
                                                  .title("squirlex galore")
                                                  .creatorName("avieciie")
                                                  .trackCount(4)
                                                  .likesCount(2)
                                                  .createdAt(new Date())
                                                  .isPrivate(false).build();
        return PlaylistFixtures.playlistItemBuilder(playlist).build();
    }

    public static PlaylistItem expectedPostedPlaylistsForPostedPlaylistsScreen() {
        return postedPlaylistForPostedPlaylistScreen(Urn.forPlaylist(123L)).build();
    }

    private static PlaylistItem.Builder postedPlaylistForPostedPlaylistScreen(Urn playlistUrn) {
        final Playlist playlist = PlaylistFixtures.playlistBuilder()
                                                  .urn(playlistUrn)
                                                  .title("squirlex galore")
                                                  .creatorName("avieciie")
                                                  .trackCount(4)
                                                  .likesCount(2)
                                                  .createdAt(new Date())
                                                  .isPrivate(false)
                                                  .permalinkUrl("http://permalink.url").build();
        return PlaylistFixtures.playlistItemBuilder(playlist);
    }

    public static PlaylistItem expectedPostedPlaylistForPostsScreen() {
        return expectedPostedPlaylistsForPostedPlaylistsScreen();
    }

    public static LastPostedTrack expectedLastPostedTrackForPostsScreen() {
        return LastPostedTrack.create(Urn.forTrack(123L), new Date(), "http://permalink.url");
    }

    private static PlaylistItem.Builder basePromotedPlaylist(PromotedProperties promotedProperties) {
        final Playlist playlist = PlaylistFixtures.playlistBuilder()
                                                  .urn(Urn.forPlaylist(123L))
                                                  .title("squirlex galore")
                                                  .creatorName("avieciie")
                                                  .trackCount(4)
                                                  .likesCount(2)
                                                  .createdAt(new Date())
                                                  .isPrivate(false)
                                                  .permalinkUrl("http://permalink.url").build();
        return PlaylistFixtures.playlistItemBuilder(playlist).promotedProperties(promotedProperties);
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
        final Track track = TrackFixtures.trackBuilder()
                                         .urn(trackUrn)
                                         .creatorUrn(creatorUrn)
                                         .policy(policy)
                                         .monetizationModel(MONETIZATION_MODEL)
                                         .snippetDuration(duration)
                                         .fullDuration(duration)
                                         .snipped(false)
                                         .build();
        return ModelFixtures.entityItemCreator().trackItem(track);
    }

    public static TrackItem expectedTrackForAnalytics(Urn trackUrn, Urn creatorUrn) {
        return expectedTrackForAnalytics(trackUrn, creatorUrn, "ALLOW", 1000);
    }

    public static TrackItem fromApiTrack() {
        return fromApiTrack(TrackFixtures.apiTrack());
    }

    public static TrackItem fromApiTrackWithReposter(String reposter, Urn reposterUrn) {
        return builderFromApiTrack(TrackFixtures.apiTrack()).repostedProperties(RepostedProperties.create(reposter, reposterUrn)).build();
    }

    public static TrackItem fromApiTrack(ApiTrack apiTrack) {
        return builderFromApiTrack(apiTrack).build();
    }

    private static TrackItem.Builder builderFromApiTrack(ApiTrack apiTrack) {
        return builderFromApiTrack(apiTrack, false, false, false);
    }

    private static TrackItem.Builder builderFromApiTrack(ApiTrack apiTrack, boolean isPrivate, boolean isLiked, boolean isReposted) {
        final Track.Builder builder = Track.from(apiTrack).toBuilder();
        builder.isPrivate(isPrivate);
        builder.userLike(isLiked);
        builder.userRepost(isReposted);
        return ModelFixtures.entityItemCreator().trackItem(builder.build()).toBuilder();
    }

    public static PlaylistItem fromApiPlaylistWithReposter(String reposter, Urn reposterUrn) {
        final RepostedProperties repostedProperties = RepostedProperties.create(reposter, reposterUrn);
        return fromApiPlaylist(PlaylistFixtures.apiPlaylist(), false, false, false, Optional.of(repostedProperties));
    }

    public static PlaylistItem fromApiPlaylist(ApiPlaylist apiPlaylist,
                                               boolean isLiked,
                                               boolean isReposted,
                                               boolean markedForOffline,
                                               Optional<RepostedProperties> repostedProperties) {
        final PlaylistItem.Builder playlistItem = PlaylistFixtures.playlistItem(apiPlaylist).toBuilder();
        playlistItem.isUserLike(isLiked);
        playlistItem.isUserRepost(isReposted);
        playlistItem.isMarkedForOffline(markedForOffline);
        playlistItem.repostedProperties(repostedProperties);
        return playlistItem.build();
    }

    public static StreamEntity timelineItem(Date createdAt) {
        final ApiTrack apiTrack = TrackFixtures.apiTrack();
        final Urn urn = apiTrack.getUrn();
        return StreamEntity.builder(urn, createdAt).build();
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
                           .imageUrlTemplate(Optional.absent())
                           .playableTitle("sounds of ze forzz")
                           .userIsPro(false);

    }

    public static TrackItem highTierTrack() {
        final Track.Builder builder = highTierTrackBuilder();
        return ModelFixtures.entityItemCreator().trackItem(builder.build());
    }

    public static Track.Builder highTierTrackBuilder() {
        return TrackFixtures.trackBuilder().subHighTier(true);
    }

    public static TrackItem upsellableTrack() {
        return ModelFixtures.entityItemCreator().trackItem(upsellableTrackBuilder().build());
    }

    public static Track.Builder upsellableTrackBuilder() {
        final Track.Builder builder = highTierTrackBuilder();
        builder.snipped(true);
        return builder;
    }
}
