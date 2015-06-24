package com.soundcloud.android.api.model;

import com.soundcloud.android.api.model.stream.ApiStreamTrackPost;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiStreamTrackPost.class)
public class ApiTrackPostBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiStreamTrackPost(
                    ModelFixtures.create(ApiTrack.class),
                    new Date()
            );
        }
    };
}
