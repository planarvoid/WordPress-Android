package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.stream.ApiTrackPost;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(ApiTrackPost.class)
public class ApiTrackPostBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiTrackPost(
                    ModelFixtures.create(ApiTrack.class)
            );
        }
    };
}
