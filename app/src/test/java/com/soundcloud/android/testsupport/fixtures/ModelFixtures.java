package com.soundcloud.android.testsupport.fixtures;

import static com.soundcloud.android.testsupport.TrackFixtures.apiTrack;
import static com.soundcloud.android.testsupport.TrackFixtures.trackBuilder;
import static com.soundcloud.android.testsupport.TrackFixtures.trackItem;
import static com.soundcloud.android.testsupport.TrackFixtures.trackItemBuilder;
import static com.soundcloud.java.optional.Optional.of;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.legacy.model.PublicApiCommentBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiTrackBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiUserBlueprint;
import com.soundcloud.android.api.legacy.model.RecordingBlueprint;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPostBlueprint;
import com.soundcloud.android.api.model.ApiPlaylistRepostBlueprint;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPostBlueprint;
import com.soundcloud.android.api.model.ApiTrackRepostBlueprint;
import com.soundcloud.android.api.model.ApiTrackStatsBlueprint;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.comments.ApiComment;
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.configuration.ConfigurationBlueprint;
import com.soundcloud.android.configuration.experiments.AssignmentBlueprint;
import com.soundcloud.android.discovery.systemplaylist.ApiSystemPlaylist;
import com.soundcloud.android.events.PlaybackSessionEventBlueprint;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserUrnBlueprint;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.IOfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.TrackingMetadata;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.policies.ApiPolicyInfo;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.profile.ApiPlayableSource;
import com.soundcloud.android.search.ApiUniversalSearchItem;
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
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.TestOfflinePropertiesProvider;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.User;
import com.soundcloud.java.optional.Optional;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import com.tobedevoured.modelcitizen.RegisterBlueprintException;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ModelFixtures {

    private static final ModelFactory modelFactory = new ModelFactory();

    public static EntityItemCreator entityItemCreator() {
        return new EntityItemCreator(testOfflinePropertiesProvider());
    }

    @NonNull
    public static IOfflinePropertiesProvider testOfflinePropertiesProvider() {
        return new TestOfflinePropertiesProvider(new OfflineProperties());
    }

    static {
        try {
            modelFactory.registerBlueprint(PublicApiUserBlueprint.class);
            modelFactory.registerBlueprint(UserUrnBlueprint.class);
            modelFactory.registerBlueprint(PublicApiTrackBlueprint.class);
            modelFactory.registerBlueprint(RecordingBlueprint.class);
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

    /**
     * @deprecated Use specific method for the class you want
     */
    @Deprecated
    public static <T> T create(Class<T> target) {
        try {
            return modelFactory.createModel(target);
        } catch (CreateModelException e) {
            throw new RuntimeException("Failed creating model of type " + target, e);
        }
    }

    /**
     * @deprecated Use specific method for the class you want
     */
    @Deprecated
    public static <T> List<T> create(Class<T> target, int count) {
        List<T> models = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            models.add(create(target));
        }
        return models;
    }

    public static Configuration configuration() {
        return create(Configuration.class);
    }

    public static ApiSystemPlaylist apiSystemPlaylist() {
        return ApiSystemPlaylist.create(Urn.forSystemPlaylist("123"),
                                        Optional.of(1),
                                        Optional.of(new Date(123)),
                                        Optional.of("title"),
                                        Optional.of("description"),
                                        Optional.of("http://fancy.jpg"),
                                        Optional.of("The Upload"),
                                        new ModelCollection<>(TrackFixtures.apiTracks(1)));
    }

    public static ApiLike apiTrackLike() {
        return apiTrackLike(apiTrack());
    }

    public static ApiLike apiTrackLike(ApiTrack apiTrack) {
        return ApiLike.create(apiTrack.getUrn(), new Date());
    }

    public static ApiLike apiPlaylistLike() {
        return apiPlaylistLike(PlaylistFixtures.apiPlaylist());
    }

    public static ApiLike apiPlaylistLike(ApiPlaylist apiPlaylist) {
        return ApiLike.create(apiPlaylist.getUrn(), new Date());
    }

    public static ApiPlayableSource apiTrackHolder() {
        return ApiPlayableSource.create(apiTrack(), null);
    }

    public static ApiPlayableSource apiPlaylistHolder() {
        return ApiPlayableSource.create(null, PlaylistFixtures.apiPlaylist());
    }

    public static ApiPlaylistWithTracks apiPlaylistWithNoTracks() {
        return new ApiPlaylistWithTracks(
                PlaylistFixtures.apiPlaylist(),
                new ModelCollection<>()
        );
    }

    public static ApiPlaylistWithTracks apiPlaylistWithTracks(List<ApiTrack> tracks) {
        return new ApiPlaylistWithTracks(
                PlaylistFixtures.apiPlaylist(),
                new ModelCollection<>(tracks)
        );
    }

    public static ApiPostItem apiTrackPostItem() {
        return new ApiPostItem(apiTrackPost(apiTrack()), null, null, null);
    }

    public static ApiPost apiTrackPost(ApiTrack apiTrack) {
        return ApiPost.create(apiTrack.getUrn(), new Date());
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
        return new ApiTrackLikeActivity(track, UserFixtures.apiUser(), createdAt);
    }

    public static ApiTrackLikeActivity apiTrackLikeActivity(Date createdAt) {
        return apiTrackLikeActivity(apiTrack(), createdAt);
    }

    public static ApiTrackCommentActivity apiTrackCommentActivity(Date createdAt) {
        final ApiComment comment = apiComment(Urn.forComment(213));
        final ApiTrack track = apiTrack();
        return new ApiTrackCommentActivity(comment.getUrn().toString(), track, comment, createdAt);
    }

    public static ApiUserFollowActivity apiUserFollowActivity(Date createdAt) {
        return new ApiUserFollowActivity( UserFixtures.apiUser(), createdAt);
    }

    public static ApiPlaylistRepostActivity apiPlaylistRepostActivity(ApiPlaylist playlist) {
        return new ApiPlaylistRepostActivity(playlist,  UserFixtures.apiUser(), new Date());
    }

    public static ApiActivityItem apiActivityWithRepostedTrack(ApiTrack track) {
        final ApiTrackRepostActivity trackRepost = new ApiTrackRepostActivity(track,  UserFixtures.apiUser(), new Date());
        return ApiActivityItem.builder().trackRepost(trackRepost).build();
    }

    public static ApiActivityItem apiActivityWithLikedPlaylist(ApiPlaylist playlist) {
        final ApiPlaylistLikeActivity playlistLike = new ApiPlaylistLikeActivity(playlist,  UserFixtures.apiUser(), new Date());
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

    public static PublicApiComment publicApiComment() {
        return ModelFixtures.create(PublicApiComment.class);
    }

    public static ApiComment apiComment(Urn urn) {
        return apiComment(urn, apiTrack().getUrn(), UserFixtures.apiUser());
    }

    public static ApiComment apiComment(Urn commentUrn, Urn trackUrn, ApiUser byUser) {
        return ApiComment.builder()
                         .urn(commentUrn)
                         .trackUrn(trackUrn)
                         .body("Great stuff!")
                         .createdAt(new Date())
                         .trackTime(Optional.of(1234L))
                         .user(byUser)
                         .build();
    }

    public static TrackItem trackItemWithOfflineState(Urn trackUrn, OfflineState state) {
        final TrackItem.Builder builder = trackItemBuilder(trackUrn);
        builder.offlineState(state);
        return builder.build();
    }

    public static TrackItem trackItemWithOfflineState(ApiTrack apiTrack, OfflineState state) {
        final TrackItem.Builder builder = trackItemBuilder(apiTrack);
        builder.offlineState(state);
        return builder.build();
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
        return PlaylistItem.from(playlist, streamEntity, new OfflineProperties());
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
            return TrackStreamItem.Companion.create(ModelFixtures.entityItemCreator().trackItem(track, streamEntity), streamEntity.createdAt(), avatarUrlTemplate);
        } else {
            return TrackStreamItem.Companion.create(ModelFixtures.entityItemCreator().trackItem(track), streamEntity.createdAt(), avatarUrlTemplate);
        }
    }

    public static ListItem listItemFromSearchItem(ApiUniversalSearchItem searchItem) {
        EntityItemCreator entityItemCreator = new EntityItemCreator(testOfflinePropertiesProvider());
        if (searchItem.track().isPresent()) {
            return entityItemCreator.trackItem(searchItem.track().get());
        } else if (searchItem.playlist().isPresent()) {
            return entityItemCreator.playlistItem(searchItem.playlist().get());
        } else if (searchItem.user().isPresent()) {
            return entityItemCreator.userItem(searchItem.user().get(), false);
        } else {
            throw new RuntimeException("Unknown search item type " + searchItem);
        }
    }
}
