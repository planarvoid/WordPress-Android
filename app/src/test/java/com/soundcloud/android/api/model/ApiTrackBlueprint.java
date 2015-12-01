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
    long duration = 12345L;

    @Default(force = true)
    long fullDuration = 678910L;

    @Default
    String artworkUrl = "http://i1.sndcdn.com/artworks-000056989650-zm98k6-large.jpg?5e64f12";

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
            if (model.getUser() == null){
                model.setUser(ModelFixtures.create(ApiUser.class));
            }
            model.setMonetizationModel("SUB_MID_TIER");
            model.setSubMidTier(true);
            model.setSubHighTier(true);
            return model;
        }
    };
}
