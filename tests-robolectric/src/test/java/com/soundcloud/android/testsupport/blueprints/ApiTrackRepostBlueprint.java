package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiTrackRepost;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiTrackRepost.class)
public class ApiTrackRepostBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiTrackRepost(
                    ModelFixtures.create(ApiTrack.class),
                    ModelFixtures.create(ApiUser.class),
                    new Date()
            );
        }
    };
}
