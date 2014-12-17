package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.likes.ApiTrackLike;
import com.soundcloud.android.model.Urn;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiTrackLike.class)
public class ApiTrackLikeBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiTrackLike(Urn.forTrack(123L), new Date());
        }
    };
}
