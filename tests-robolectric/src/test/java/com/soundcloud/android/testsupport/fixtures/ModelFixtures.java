package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.ads.ApiAudioAdBlueprint;
import com.soundcloud.android.ads.ApiCompanionAdBlueprint;
import com.soundcloud.android.ads.ApiDisplayPropertiesBlueprint;
import com.soundcloud.android.ads.ApiInterstitialBlueprint;
import com.soundcloud.android.ads.ApiLeaveBehindBlueprint;
import com.soundcloud.android.api.legacy.model.AffiliationActivityBlueprint;
import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiCommentBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylistBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiTrackBlueprint;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.PublicApiUserBlueprint;
import com.soundcloud.android.api.legacy.model.RecordingBlueprint;
import com.soundcloud.android.api.model.ApiTrackStatsBlueprint;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistBlueprint;
import com.soundcloud.android.api.model.ApiPlaylistPostBlueprint;
import com.soundcloud.android.api.model.ApiPlaylistRepostBlueprint;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackBlueprint;
import com.soundcloud.android.api.model.ApiTrackPostBlueprint;
import com.soundcloud.android.api.model.ApiTrackRepostBlueprint;
import com.soundcloud.android.api.model.ApiUserBlueprint;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.configuration.ConfigurationBlueprint;
import com.soundcloud.android.configuration.experiments.AssignmentBlueprint;
import com.soundcloud.android.events.PlaybackSessionEventBlueprint;
import com.soundcloud.android.model.UserUrnBlueprint;
import com.soundcloud.android.onboarding.suggestions.CategoryBlueprint;
import com.soundcloud.android.onboarding.suggestions.SuggestedUserBlueprint;
import com.soundcloud.android.playlists.PlaylistItemBlueprint;
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
            modelFactory.registerBlueprint(CategoryBlueprint.class);
            modelFactory.registerBlueprint(SuggestedUserBlueprint.class);
            modelFactory.registerBlueprint(ApiTrackBlueprint.class);
            modelFactory.registerBlueprint(ApiPlaylistBlueprint.class);
            modelFactory.registerBlueprint(ApiTrackStatsBlueprint.class);
            modelFactory.registerBlueprint(PublicApiPlaylistBlueprint.class);
            modelFactory.registerBlueprint(PlaybackSessionEventBlueprint.class);
            modelFactory.registerBlueprint(AssignmentBlueprint.class);
            modelFactory.registerBlueprint(PublicApiCommentBlueprint.class);
            modelFactory.registerBlueprint(AffiliationActivityBlueprint.class);
            modelFactory.registerBlueprint(ApiAudioAdBlueprint.class);
            modelFactory.registerBlueprint(ApiCompanionAdBlueprint.class);
            modelFactory.registerBlueprint(ApiDisplayPropertiesBlueprint.class);
            modelFactory.registerBlueprint(ApiLeaveBehindBlueprint.class);
            modelFactory.registerBlueprint(ApiInterstitialBlueprint.class);
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

    public static ApiPlaylistWithTracks apiPlaylistWithNoTracks(){
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

}
