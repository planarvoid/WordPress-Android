package com.soundcloud.android.api.model;

import com.soundcloud.android.api.model.stream.ApiStreamTrackRepost;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.testsupport.UserFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiStreamTrackRepost.class)
public class ApiTrackRepostBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiStreamTrackRepost(
                    TrackFixtures.apiTrack(),
                    UserFixtures.apiUser(),
                    new Date()
            );
        }
    };
}
