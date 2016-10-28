package com.soundcloud.android.api.model;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.callback.AfterCreateCallback;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiTrack.class)
public class ApiTrackBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiTrack(Urn.forTrack(runningId++));
        }
    };

    @Default
    String title = "new track " + System.currentTimeMillis();

    @Mapped
    ApiTrackStats stats;

    @Default
    String streamUrl = "http://media.soundcloud.com/stream/whVhoRw2gpUh";

    @Default(force = true)
    long snippetDuration = 12345L;

    @Default(force = true)
    long fullDuration = 678910L;

    @Default
    String waveformUrl = "http://waveform.url";

    @Default
    String permalinkUrl = "https://soundcloud.com/bismakarisma/kuatkitabersinar";

    @Default(force = true)
    Sharing sharing = Sharing.PUBLIC;

    @Default
    Date createdAt = new Date();

    @Default
    String genre = "Clownstep";

    @Default
    String policy = "allowed";

    // avoid the setter problem where getters and setters are typed differently
    // https://github.com/mguymon/model-citizen/issues/20
    AfterCreateCallback<ApiTrack> afterCreate = new AfterCreateCallback<ApiTrack>() {
        @Override
        public ApiTrack afterCreate(ApiTrack model) {
            if (model.getUser() == null) {
                model.setUser(ModelFixtures.create(ApiUser.class));
            }
            model.setArtworkUrlTemplate("https://i1.sndcdn.com/artworks-" + runningId + "-{size}.jpg");
            model.setMonetizationModel("MONETIZABLE");
            model.setSubMidTier(false);
            model.setSubHighTier(false);
            return model;
        }
    };
}
