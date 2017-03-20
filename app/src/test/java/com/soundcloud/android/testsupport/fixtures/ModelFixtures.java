package com.soundcloud.android.testsupport.fixtures;

import static com.soundcloud.java.optional.Optional.of;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.legacy.model.PublicApiCommentBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiTrackBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiUserBlueprint;
import com.soundcloud.android.api.legacy.model.RecordingBlueprint;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistBlueprint;
import com.soundcloud.android.api.model.ApiPlaylistPostBlueprint;
import com.soundcloud.android.api.model.ApiPlaylistRepostBlueprint;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackBlueprint;
import com.soundcloud.android.api.model.ApiTrackPostBlueprint;
import com.soundcloud.android.api.model.ApiTrackRepostBlueprint;
import com.soundcloud.android.api.model.ApiTrackStatsBlueprint;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ApiUserBlueprint;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.comments.ApiComment;
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.configuration.ConfigurationBlueprint;
import com.soundcloud.android.configuration.experiments.AssignmentBlueprint;
import com.soundcloud.android.events.PlaybackSessionEventBlueprint;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserUrnBlueprint;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.TrackingMetadata;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.policies.ApiPolicyInfo;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.profile.ApiPlayableSource;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.topresults.SearchItem;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.stream.TrackStreamItem;
import com.soundcloud.android.sync.activities.ApiActivityItem;
import com.soundcloud.android.sync.activities.ApiPlaylistLikeActivity;
import com.soundcloud.android.sync.activities.ApiPlaylistRepostActivity;
import com.soundcloud.android.sync.activities.ApiTrackCommentActivity;
import com.soundcloud.android.sync.activities.ApiTrackLikeActivity;
import com.soundcloud.android.sync.activities.ApiTrackRepostActivity;
import com.soundcloud.android.sync.activities.ApiUserFollowActivity;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.sync.playlists.ApiPlaylistWithTracks;
import com.soundcloud.android.sync.posts.ApiPost;
import com.soundcloud.android.sync.posts.ApiPostItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import com.tobedevoured.modelcitizen.RegisterBlueprintException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ModelFixtures {

    public static long runningUserId = 1L;

    private static final ModelFactory modelFactory = new ModelFactory();

    public static EntityItemCreator entityItemCreator() {
        return new EntityItemCreator();
    }

    static {
        try {
            modelFactory.registerBlueprint(PublicApiUserBlueprint.class);
            modelFactory.registerBlueprint(UserUrnBlueprint.class);
            modelFactory.registerBlueprint(ApiUserBlueprint.class);
            modelFactory.registerBlueprint(PublicApiTrackBlueprint.class);
            modelFactory.registerBlueprint(RecordingBlueprint.class);
            modelFactory.registerBlueprint(ApiTrackBlueprint.class);
            modelFactory.registerBlueprint(ApiPlaylistBlueprint.class);
            modelFactory.registerBlueprint(ApiTrackStatsBlueprint.class);
            modelFactory.registerBlueprint(PlaybackSessionEventBlueprint.class);
            modelFactory.registerBlueprint(AssignmentBlueprint.class);
            modelFactory.registerBlueprint(PublicApiCommentBlueprint.class);
            modelFactory.registerBlueprint(ApiTrackPostBlueprint.class);
            modelFactory.registerBlueprint(ApiTrackRepostBlueprint.class);
            modelFactory.registerBlueprint(ApiPlaylistPostBlueprint.class);
            modelFactory.registerBlueprint(ApiPlaylistRepostBlueprint.class);
            modelFactory.registerBlueprint(ConfigurationBlueprint.class);
        } catch (RegisterBlueprintException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T create(Class<T> target) {
        try {
            return modelFactory.createModel(target);
        } catch (CreateModelException e) {
            throw new RuntimeException("Failed creating model of type " + target, e);
        }
    }

    public static <T> List<T> create(Class<T> target, int count) {
        List<T> models = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            models.add(create(target));
        }
        return models;
    }

    public static ApiTrack apiTrack() {
        return create(ApiTrack.class);
    }

    public static ApiUser apiUser() {
        return create(ApiUser.class);
    }

    public static Configuration configuration() {
        return create(Configuration.class);
    }

    public static ApiPlaylist apiPlaylist() {
        return create(ApiPlaylist.class);
    }

    public static ApiPlaylist apiAlbum() {
        final ApiPlaylist apiPlaylist = create(ApiPlaylist.class);

        apiPlaylist.setIsAlbum(true);

        return apiPlaylist;
    }

    public static Playlist playlist() {
        return playlistBuilder().trackCount(0).build();
    }

    public static Playlist playlist(Urn urn) {
        return playlistBuilder().urn(urn).build();
    }

    public static Playlist album() {
        return playlistBuilder().isAlbum(true).build();
    }

    public static Playlist.Builder playlistBuilder() {
        return playlistBuilder(ModelFixtures.create(ApiPlaylist.class));
    }

    public static Playlist.Builder playlistBuilder(ApiPlaylist apiPlaylist) {
        return Playlist.builder()
                       .permalinkUrl(Optional.of(apiPlaylist.getPermalinkUrl()))
                       .repostCount(apiPlaylist.getRepostsCount())
                       .likesCount(apiPlaylist.getLikesCount())
                       .creatorName(apiPlaylist.getUsername())
                       .creatorUrn(apiPlaylist.getUser().getUrn())
                       .duration(apiPlaylist.getDuration())
                       .imageUrlTemplate(apiPlaylist.getImageUrlTemplate())
                       .isPrivate(apiPlaylist.getSharing().isPrivate())
                       .title(apiPlaylist.getTitle())
                       .trackCount(apiPlaylist.getTrackCount())
                       .urn(apiPlaylist.getUrn())
                       .createdAt(apiPlaylist.getCreatedAt())
                       .isAlbum(apiPlaylist.isAlbum())
                       .setType(apiPlaylist.getSetType())
                       .genre(apiPlaylist.getGenre())
                       .releaseDate(apiPlaylist.getReleaseDate());
    }

    public static Playlist.Builder playlistBuilder(Playlist apiPlaylist) {
        return Playlist.builder()
                       .permalinkUrl(apiPlaylist.permalinkUrl())
                       .repostCount(apiPlaylist.repostCount())
                       .likesCount(apiPlaylist.likesCount())
                       .creatorName(apiPlaylist.creatorName())
                       .creatorUrn(apiPlaylist.creatorUrn())
                       .duration(apiPlaylist.duration())
                       .imageUrlTemplate(apiPlaylist.imageUrlTemplate())
                       .isPrivate(apiPlaylist.isPrivate())
                       .title(apiPlaylist.title())
                       .trackCount(apiPlaylist.trackCount())
                       .urn(apiPlaylist.urn())
                       .createdAt(apiPlaylist.createdAt())
                       .isAlbum(apiPlaylist.isAlbum())
                       .setType(apiPlaylist.setType())
                       .genre(apiPlaylist.genre())
                       .releaseDate(apiPlaylist.releaseDate());
    }

    public static User user() {
        return user(false);
    }

    public static User user(Urn urn) {
        return userBuilder(false).urn(urn).build();
    }

    public static User user(boolean isFollowing) {
        return userBuilder(isFollowing)
                .build();
    }

    public static UserItem userItem() {
        return userItem(apiUser());
    }

    public static UserItem userItem(ApiUser user) {
        return userItem(user(user));
    }

    public static User user(ApiUser user) {
        return User.fromApiUser(user);
    }

    public static UserItem userItem(User user) {
        return UserItem.from(user);
    }

    public static UserItem userItem(Urn urn) {
        return UserItem.from(user(urn));
    }

    public static User.Builder userBuilder() {
        return userBuilder(false);
    }

    public static User.Builder userBuilder(boolean isFollowing) {
        return User.builder()
                   .urn(Urn.forUser(runningUserId++))
                   .username("avieciie")
                   .country(of("country"))
                   .city(of("city"))
                   .followersCount(2)
                   .followingsCount(6)
                   .isFollowing(isFollowing)
                   .avatarUrl(of("avatar-url"))
                   .visualUrl(of("visual-url"));
    }

    public static ApiLike apiTrackLike() {
        return apiTrackLike(ModelFixtures.create(ApiTrack.class));
    }

    public static ApiLike apiTrackLike(ApiTrack apiTrack) {
        return ApiLike.create(apiTrack.getUrn(), new Date());
    }

    public static ApiLike apiPlaylistLike() {
        return apiPlaylistLike(ModelFixtures.create(ApiPlaylist.class));
    }

    public static ApiLike apiPlaylistLike(ApiPlaylist apiPlaylist) {
        return ApiLike.create(apiPlaylist.getUrn(), new Date());
    }

    public static ApiPlayableSource apiTrackHolder() {
        return ApiPlayableSource.create(ModelFixtures.create(ApiTrack.class), null);
    }

    public static ApiPlayableSource apiPlaylistHolder() {
        return ApiPlayableSource.create(null, ModelFixtures.create(ApiPlaylist.class));
    }

    public static ApiPlaylistWithTracks apiPlaylistWithNoTracks() {
        return new ApiPlaylistWithTracks(
                ModelFixtures.create(ApiPlaylist.class),
                new ModelCollection<>()
        );
    }

    public static ApiPlaylistWithTracks apiPlaylistWithTracks(List<ApiTrack> tracks) {
        return new ApiPlaylistWithTracks(
                ModelFixtures.create(ApiPlaylist.class),
                new ModelCollection<>(tracks)
        );
    }

    public static ApiPostItem apiTrackPostItem() {
        return new ApiPostItem(apiTrackPost(ModelFixtures.create(ApiTrack.class)), null, null, null);
    }

    public static ApiPost apiTrackPost(ApiTrack apiTrack) {
        return ApiPost.create(apiTrack.getUrn(), new Date());
    }

    public static TrackItem trackItem(ApiTrack track) {
        return trackItem(Track.from(track));
    }

    public static TrackItem trackItem(Track track) {
        return entityItemCreator().trackItem(track);
    }

    public static TrackItem trackItem(Track track, StreamEntity streamEntity) {
        return entityItemCreator().trackItem(track, streamEntity);
    }

    public static TrackItem trackItem() {
        return TrackItem.builder(track()).build();
    }

    public static TrackItem.Builder trackItemBuilder() {
        return ModelFixtures.trackItem().toBuilder();
    }

    public static TrackItem.Builder trackItemBuilder(Urn urn) {
        return ModelFixtures.entityItemCreator().trackItem(trackBuilder().urn(urn).build()).toBuilder();
    }

    public static TrackItem.Builder trackItemBuilder(ApiTrack apiTrack) {
        return ModelFixtures.entityItemCreator().trackItem(trackBuilder(apiTrack).build()).toBuilder();
    }

    public static TrackItem trackItem(Urn urn) {
        return ModelFixtures.entityItemCreator().trackItem(trackBuilder().urn(urn).build());
    }

    public static Track track() {
        return Track.from(ModelFixtures.create(ApiTrack.class));
    }

    public static List<TrackItem> trackItems(int count) {
        return Lists.transform(create(ApiTrack.class, count), ModelFixtures::trackItem);
    }

    public static List<Track> tracks(int count) {
        final List<Track> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        }
        return result;
    }

    public static List<PlaylistItem> playlistItem(int count) {
        final List<PlaylistItem> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(playlistItem());
        }
        return list;
    }

    public static PlaylistItem playlistItem() {
        return playlistItem(playlist());
    }

    public static PlaylistItem.Builder playlistItemBuilder() {
        return playlistItemBuilder(playlist());
    }

    public static PlaylistItem playlistItem(ApiPlaylist apilaylist) {
        return playlistItem(Playlist.from(apilaylist));
    }

    public static PlaylistItem.Builder playlistItemBuilder(ApiPlaylist apilaylist) {
        return playlistItemBuilder(Playlist.from(apilaylist));
    }

    public static PlaylistItem playlistItem(Playlist playlist) {
        return playlistItemBuilder(playlist).build();
    }

    public static PlaylistItem.Builder playlistItemBuilder(Playlist playlist) {
        return PlaylistItem.builder(playlist);
    }

    public static PlaylistItem playlistItem(Urn urn) {
        return playlistItem(playlist(urn));
    }

    public static ApiPolicyInfo apiPolicyInfo(Urn trackUrn) {
        return apiPolicyInfo(trackUrn, true, "policy", true);
    }

    public static ApiPolicyInfo apiPolicyInfo(Urn trackUrn, boolean monetizable, String policy, boolean syncable) {
        return ApiPolicyInfo.create(trackUrn.toString(),
                                    monetizable,
                                    policy,
                                    syncable,
                                    "model",
                                    true,
                                    true,
                                    true,
                                    true);
    }

    public static DownloadRequest downloadRequestFromLikes(ApiTrack track) {
        TrackingMetadata trackContext = new TrackingMetadata(track.getUser().getUrn(), true, false);
        return DownloadRequest.create(track.getUrn(),
                                      track.getImageUrlTemplate(),
                                      track.getFullDuration(),
                                      track.getWaveformUrl(),
                                      true,
                                      false,
                                      trackContext);
    }

    public static DownloadRequest downloadRequestFromLikes(Urn track) {
        TrackingMetadata trackContext = new TrackingMetadata(Urn.forUser(123L), true, false);
        return DownloadRequest.create(track,
                                      of("http://artwork.url"),
                                      1234,
                                      "http://waveform.url",
                                      true,
                                      false,
                                      trackContext);
    }

    public static DownloadRequest downloadRequestFromPlaylists(ApiTrack track) {
        TrackingMetadata trackContext = new TrackingMetadata(track.getUser().getUrn(), false, true);
        return DownloadRequest.create(track.getUrn(),
                                      track.getImageUrlTemplate(),
                                      track.getFullDuration(),
                                      track.getWaveformUrl(),
                                      true,
                                      false,
                                      trackContext);
    }

    public static DownloadRequest downloadRequestFromLikesAndPlaylists(ApiTrack track) {
        TrackingMetadata trackContext = new TrackingMetadata(track.getUser().getUrn(), true, true);
        return DownloadRequest.create(track.getUrn(),
                                      track.getImageUrlTemplate(),
                                      track.getFullDuration(),
                                      track.getWaveformUrl(),
                                      true,
                                      false,
                                      trackContext);
    }

    public static DownloadRequest creatorOptOutRequest(Urn track) {
        TrackingMetadata trackContext = new TrackingMetadata(Urn.forUser(123L), false, true);
        return DownloadRequest.create(track,
                                      of("http://artwork.url"),
                                      1234,
                                      "http://waveform.url",
                                      false,
                                      false,
                                      trackContext);
    }

    public static DownloadRequest snippetRequest(Urn track) {
        TrackingMetadata trackContext = new TrackingMetadata(Urn.forUser(123L), false, true);
        return DownloadRequest.create(track,
                                      of("http://artwork.url"),
                                      1234,
                                      "http://waveform.url",
                                      false,
                                      true,
                                      trackContext);
    }

    public static ApiActivityItem apiActivityWithLikedTrack(ApiTrack track) {
        return ApiActivityItem.builder().trackLike(apiTrackLikeActivity(track, new Date())).build();
    }

    public static ApiTrackLikeActivity apiTrackLikeActivity(ApiTrack track, Date createdAt) {
        final ApiUser user = create(ApiUser.class);
        return new ApiTrackLikeActivity(track, user, createdAt);
    }

    public static ApiTrackLikeActivity apiTrackLikeActivity(Date createdAt) {
        return apiTrackLikeActivity(create(ApiTrack.class), createdAt);
    }

    public static ApiTrackCommentActivity apiTrackCommentActivity(Date createdAt) {
        final ApiComment comment = apiComment(Urn.forComment(213));
        final ApiTrack track = create(ApiTrack.class);
        return new ApiTrackCommentActivity(comment.getUrn().toString(), track, comment, createdAt);
    }

    public static ApiUserFollowActivity apiUserFollowActivity(Date createdAt) {
        final ApiUser user = create(ApiUser.class);
        return new ApiUserFollowActivity(user, createdAt);
    }

    public static ApiPlaylistRepostActivity apiPlaylistRepostActivity(ApiPlaylist playlist) {
        final ApiUser user = create(ApiUser.class);
        return new ApiPlaylistRepostActivity(playlist, user, new Date());
    }

    public static ApiActivityItem apiActivityWithRepostedTrack(ApiTrack track) {
        final ApiUser reposter = create(ApiUser.class);
        final ApiTrackRepostActivity trackRepost = new ApiTrackRepostActivity(track, reposter, new Date());
        return ApiActivityItem.builder().trackRepost(trackRepost).build();
    }

    public static ApiActivityItem apiActivityWithLikedPlaylist(ApiPlaylist playlist) {
        final ApiUser user = create(ApiUser.class);
        final ApiPlaylistLikeActivity playlistLike = new ApiPlaylistLikeActivity(playlist, user, new Date());
        return ApiActivityItem.builder().playlistLike(playlistLike).build();
    }

    public static ApiActivityItem apiActivityWithRepostedPlaylist(ApiPlaylist playlist) {
        final ApiPlaylistRepostActivity playlistRepost = apiPlaylistRepostActivity(playlist);
        return ApiActivityItem.builder().playlistRepost(playlistRepost).build();
    }

    public static ApiActivityItem apiActivityWithTrackComment(ApiComment comment, ApiTrack track) {
        assertThat(comment.getTrackUrn()).isEqualTo(track.getUrn());
        final ApiTrackCommentActivity commentActivity = new ApiTrackCommentActivity(
                comment.getUrn().toString(), track, comment, new Date());
        return ApiActivityItem.builder().trackComment(commentActivity).build();
    }

    public static ApiActivityItem apiActivityWithUserFollow(ApiUser follower) {
        return ApiActivityItem.builder().userFollow(new ApiUserFollowActivity(follower, new Date())).build();
    }

    public static ApiComment apiComment(Urn urn) {
        return apiComment(urn, create(ApiTrack.class).getUrn(), create(ApiUser.class));
    }

    public static ApiComment apiComment(Urn commentUrn, Urn trackUrn, ApiUser byUser) {
        return ApiComment.builder()
                         .urn(commentUrn)
                         .trackUrn(trackUrn)
                         .body("Great stuff!")
                         .createdAt(new Date())
                         .trackTime(1234)
                         .user(byUser)
                         .build();
    }

    public static TrackItem trackItemWithOfflineState(Urn trackUrn, OfflineState state) {
        final TrackItem.Builder builder = ModelFixtures.trackItemBuilder(trackUrn);
        builder.offlineState(state);
        return builder.build();
    }

    public static TrackItem trackItemWithOfflineState(ApiTrack apiTrack, OfflineState state) {
        final TrackItem.Builder builder = ModelFixtures.trackItemBuilder(apiTrack);
        builder.offlineState(state);
        return builder.build();
    }

    public static Track.Builder trackBuilder() {
        return Track.builder(Track.from(create(ApiTrack.class)));
    }

    public static Track.Builder baseTrackBuilder() {
        return trackBuilder().urn(Urn.forTrack(123L))
                             .snippetDuration(10L)
                             .fullDuration(1000L)
                             .title("someone's favorite song")
                             .creatorName("someone's favorite band")
                             .creatorUrn(Urn.forUser(123L))
                             .userLike(false)
                             .userRepost(false)
                             .likesCount(0)
                             .permalinkUrl("http://soundcloud.com/artist/track_permalink");
    }

    public static Track.Builder trackBuilder(ApiTrack apiTrack) {
        return Track.builder(Track.from(apiTrack));
    }

    public static Track trackWithUrnAndMonetizable(Urn currentTrackUrn, boolean monetizable) {
        return trackBuilder().urn(currentTrackUrn).monetizable(monetizable).build();
    }

    public static TrackItem expectedTrackEntityForWidget() {
        return trackItem(trackBuilder().imageUrlTemplate(of("https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg")).build());
    }

    public static TrackItem expectedLikedTrackForLikesScreen() {
        return trackItemBuilder().likesCount(2).build();
    }

    public static Track expectedTrackEntityForAnalytics(Urn trackUrn, Urn creatorUrn, String policy, long duration) {
        return trackBuilder()
                .urn(trackUrn)
                .creatorUrn(creatorUrn)
                .policy(policy)
                .monetizationModel(PlayableFixtures.MONETIZATION_MODEL)
                .snippetDuration(duration)
                .fullDuration(duration)
                .snipped(false)
                .build();
    }

    public static TrackItem promotedTrackItem(Track track, User promoter) {
        final PromotedProperties promotedStreamProperties = getPromotedProperties(promoter);
        final StreamEntity streamEntity = StreamEntity.builder(track.urn(), new Date()).promotedProperties(promotedStreamProperties).build();
        return ModelFixtures.entityItemCreator().trackItem(track, streamEntity);
    }

    public static PlaylistItem promotedPlaylistItem(Playlist playlist, User promoter) {
        final PromotedProperties promotedStreamProperties = getPromotedProperties(promoter);
        final StreamEntity streamEntity = StreamEntity.builder(playlist.urn(), new Date())
                                                      .promotedProperties(promotedStreamProperties)
                                                      .build();
        return PlaylistItem.from(playlist, streamEntity);
    }

    private static PromotedProperties getPromotedProperties(User promoter) {
        final List<String> clickedUrls = asList("promoted1", "promoted2");
        final List<String> impressionUrls = asList("promoted3", "promoted4");
        final List<String> playedUrls = asList("promoted5", "promoted6");
        final List<String> promoterClickedUrls = asList("promoted7", "promoted8");
        return PromotedProperties.create("ad:urn:123", clickedUrls, impressionUrls, playedUrls, promoterClickedUrls, of(promoter.urn()), of(promoter.username()));
    }

    public static StreamItem trackFromStreamEntity(StreamEntity streamEntity) {
        final Optional<String> avatarUrlTemplate = streamEntity.avatarUrlTemplate();
        final Track track = trackBuilder().urn(streamEntity.urn())
                                          .imageUrlTemplate(avatarUrlTemplate)
                                          .build();
        if (streamEntity.isPromoted()) {
            return TrackStreamItem.create(ModelFixtures.entityItemCreator().trackItem(track, streamEntity), streamEntity.createdAt(), avatarUrlTemplate);
        } else {
            return TrackStreamItem.create(ModelFixtures.entityItemCreator().trackItem(track), streamEntity.createdAt(), avatarUrlTemplate);
        }
    }

    public static ListItem listItemFromSearchItem(ApiUniversalSearchItem searchItem) {
        EntityItemCreator entityItemCreator = new EntityItemCreator();
        if (searchItem.track().isPresent()) {
            return entityItemCreator.trackItem(searchItem.track().get());
        } else if (searchItem.playlist().isPresent()) {
            return entityItemCreator.playlistItem(searchItem.playlist().get());
        } else if (searchItem.user().isPresent()) {
            return entityItemCreator.userItem(searchItem.user().get());
        } else {
            throw new RuntimeException("Unknown search item type " + searchItem);
        }
    }

    private static SearchItem.User searchUser(int bucketPosition) {
        return SearchItem.User.create(create(UserItem.class), bucketPosition);
    }

    private static SearchItem.Playlist searchPlaylist(int bucketPosition) {
        return SearchItem.Playlist.create(playlistItem(), bucketPosition);
    }

    private static SearchItem.Track searchTrack(int bucketPosition) {
        return SearchItem.Track.create(trackItem(), bucketPosition);
    }
}
