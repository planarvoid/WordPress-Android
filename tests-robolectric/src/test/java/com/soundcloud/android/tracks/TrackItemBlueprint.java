package com.soundcloud.android.tracks;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(TrackItem.class)
public class TrackItemBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return TrackItem.from(ModelFixtures.create(ApiTrack.class).toPropertySet());
        }
    };


}
