package com.soundcloud.android.api.model;

import com.soundcloud.android.api.model.stream.ApiStreamTrackRepost;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiStreamTrackRepost.class)
public class ApiTrackRepostBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiStreamTrackRepost(
                    ModelFixtures.create(ApiTrack.class),
                    ModelFixtures.create(ApiUser.class),
                    new Date()
            );
        }
    };
}
