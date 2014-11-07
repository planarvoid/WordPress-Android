package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.stream.ApiPromotedTrack;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(ApiPromotedTrack.class)
public class ApiPromotedTrackBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiPromotedTrack(
                    ModelFixtures.create(ApiTrack.class)
            );
        }
    };
}
