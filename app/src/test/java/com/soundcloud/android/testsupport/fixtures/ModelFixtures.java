package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.configuration.ConfigurationBlueprint;
import com.soundcloud.android.configuration.experiments.AssignmentBlueprint;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.testsupport.blueprints.AffiliationActivityBlueprint;
import com.soundcloud.android.testsupport.blueprints.ApiAudioAdBlueprint;
import com.soundcloud.android.testsupport.blueprints.ApiPlaylistBlueprint;
import com.soundcloud.android.testsupport.blueprints.ApiPlaylistPostBlueprint;
import com.soundcloud.android.testsupport.blueprints.ApiPlaylistRepostBlueprint;
import com.soundcloud.android.testsupport.blueprints.ApiTrackBlueprint;
import com.soundcloud.android.testsupport.blueprints.ApiTrackPostBlueprint;
import com.soundcloud.android.testsupport.blueprints.ApiTrackRepostBlueprint;
import com.soundcloud.android.testsupport.blueprints.ApiUserBlueprint;
import com.soundcloud.android.testsupport.blueprints.CategoryBlueprint;
import com.soundcloud.android.testsupport.blueprints.DisplayPropertiesBlueprint;
import com.soundcloud.android.testsupport.blueprints.InterstitialBlueprint;
import com.soundcloud.android.testsupport.blueprints.LeaveBehindBlueprint;
import com.soundcloud.android.testsupport.blueprints.PlaybackSessionEventBlueprint;
import com.soundcloud.android.testsupport.blueprints.PublicApiCommentBlueprint;
import com.soundcloud.android.testsupport.blueprints.PublicApiPlaylistBlueprint;
import com.soundcloud.android.testsupport.blueprints.PublicApiTrackBlueprint;
import com.soundcloud.android.testsupport.blueprints.PublicApiUserBlueprint;
import com.soundcloud.android.testsupport.blueprints.RecordingBlueprint;
import com.soundcloud.android.testsupport.blueprints.SuggestedUserBlueprint;
import com.soundcloud.android.testsupport.blueprints.TrackStatsBlueprint;
import com.soundcloud.android.testsupport.blueprints.UserUrnBlueprint;
import com.soundcloud.android.testsupport.blueprints.VisualAdWithButtonBlueprint;
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
            modelFactory.registerBlueprint(TrackStatsBlueprint.class);
            modelFactory.registerBlueprint(PublicApiPlaylistBlueprint.class);
            modelFactory.registerBlueprint(PlaybackSessionEventBlueprint.class);
            modelFactory.registerBlueprint(AssignmentBlueprint.class);
            modelFactory.registerBlueprint(PublicApiCommentBlueprint.class);
            modelFactory.registerBlueprint(AffiliationActivityBlueprint.class);
            modelFactory.registerBlueprint(ApiAudioAdBlueprint.class);
            modelFactory.registerBlueprint(VisualAdWithButtonBlueprint.class);
            modelFactory.registerBlueprint(DisplayPropertiesBlueprint.class);
            modelFactory.registerBlueprint(LeaveBehindBlueprint.class);
            modelFactory.registerBlueprint(InterstitialBlueprint.class);
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

}
