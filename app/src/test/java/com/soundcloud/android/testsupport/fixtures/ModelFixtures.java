package com.soundcloud.android.testsupport.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiCommentBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylistBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiTrackBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.PublicApiUserBlueprint;
import com.soundcloud.android.api.legacy.model.RecordingBlueprint;
import com.soundcloud.android.api.legacy.model.UserAssociation;
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
import com.soundcloud.android.configuration.ConfigurationBlueprint;
import com.soundcloud.android.configuration.experiments.AssignmentBlueprint;
import com.soundcloud.android.events.PlaybackSessionEventBlueprint;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserUrnBlueprint;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineTrackContext;
import com.soundcloud.android.playlists.PlaylistItemBlueprint;
import com.soundcloud.android.policies.ApiPolicyInfo;
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
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemBlueprint;
import com.soundcloud.android.users.UserItemBlueprint;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import com.tobedevoured.modelcitizen.RegisterBlueprintException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ModelFixtures {

    private static final ModelFactory modelFactory = new ModelFactory();

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
            modelFactory.registerBlueprint(PublicApiPlaylistBlueprint.class);
            modelFactory.registerBlueprint(PlaybackSessionEventBlueprint.class);
            modelFactory.registerBlueprint(AssignmentBlueprint.class);
            modelFactory.registerBlueprint(PublicApiCommentBlueprint.class);
            modelFactory.registerBlueprint(ApiTrackPostBlueprint.class);
            modelFactory.registerBlueprint(ApiTrackRepostBlueprint.class);
            modelFactory.registerBlueprint(ApiPlaylistPostBlueprint.class);
            modelFactory.registerBlueprint(ApiPlaylistRepostBlueprint.class);
            modelFactory.registerBlueprint(ConfigurationBlueprint.class);
            modelFactory.registerBlueprint(TrackItemBlueprint.class);
            modelFactory.registerBlueprint(PlaylistItemBlueprint.class);
            modelFactory.registerBlueprint(UserItemBlueprint.class);
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

    public static List<UserAssociation> createDirtyFollowings(int count) {
        List<UserAssociation> userAssociations = new ArrayList<>();
        for (PublicApiUser user : create(PublicApiUser.class, count)) {
            final UserAssociation association = new UserAssociation(Association.Type.FOLLOWING, user);
            association.markForAddition();
            userAssociations.add(association);
        }
        return userAssociations;
    }

    public static ApiLike apiTrackLike() {
        return apiTrackLike(ModelFixtures.create(ApiTrack.class));
    }

    public static ApiLike apiTrackLike(ApiTrack apiTrack) {
        return new ApiLike(apiTrack.getUrn(), new Date());
    }

    public static ApiLike apiPlaylistLike() {
        return apiPlaylistLike(ModelFixtures.create(ApiPlaylist.class));
    }

    public static ApiLike apiPlaylistLike(ApiPlaylist apiPlaylist) {
        return new ApiLike(apiPlaylist.getUrn(), new Date());
    }

    public static ApiPlaylistWithTracks apiPlaylistWithNoTracks() {
        return new ApiPlaylistWithTracks(
                ModelFixtures.create(ApiPlaylist.class),
                new ModelCollection<ApiTrack>()
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
        return new ApiPost(apiTrack.getUrn(), new Date());
    }

    public static List<TrackItem> trackItems(int count) {
        return TrackItem.fromApiTracks().call(create(ApiTrack.class, count));
    }

    public static ApiPolicyInfo apiPolicyInfo(Urn trackUrn) {
        return apiPolicyInfo(trackUrn, true, "policy", true);
    }

    public static ApiPolicyInfo apiPolicyInfo(Urn trackUrn, boolean monetizable, String policy, boolean syncable) {
        return ApiPolicyInfo.create(trackUrn.toString(), monetizable, policy, syncable, "model", true, true);
    }

    public static DownloadRequest downloadRequestFromLikes(Urn track) {
        OfflineTrackContext trackContext = OfflineTrackContext.create(track, Urn.forUser(123L), Collections.<Urn>emptyList(), true);
        return DownloadRequest.create(trackContext, 1234, "http://waveform.url", true);
    }

    public static DownloadRequest downloadRequestFromPlaylists(ApiTrack track, boolean inLikes, List<Urn> inPlaylists) {
        OfflineTrackContext trackContext = OfflineTrackContext.create(track.getUrn(), track.getUser().getUrn(), inPlaylists, inLikes);
        return DownloadRequest.create(trackContext, track.getDuration(), track.getWaveformUrl(), true);
    }

    public static DownloadRequest creatorOptOutRequest(Urn track) {
        OfflineTrackContext trackContext = OfflineTrackContext.create(track, Urn.forUser(123L), Collections.<Urn>emptyList(), true);
        return DownloadRequest.create(trackContext, 1234, "http://waveform.url", false);
    }

    public static DownloadRequest creatorOptOutRequest(ApiTrack track, boolean inLikes, List<Urn> inPlaylist) {
        OfflineTrackContext trackContext = OfflineTrackContext.create(track.getUrn(), track.getUser().getUrn(), inPlaylist, inLikes);
        return DownloadRequest.create(trackContext, track.getDuration(), track.getWaveformUrl(), false);
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

    public static ApiPlaylistRepostActivity apiPlaylistRepostActivity(ApiPlaylist playlist){
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
}
