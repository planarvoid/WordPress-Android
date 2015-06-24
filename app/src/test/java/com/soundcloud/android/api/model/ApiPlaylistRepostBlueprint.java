package com.soundcloud.android.api.model;

import com.soundcloud.android.api.model.stream.ApiStreamPlaylistRepost;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiStreamPlaylistRepost.class)
public class ApiPlaylistRepostBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiStreamPlaylistRepost(
                    ModelFixtures.create(ApiPlaylist.class),
                    ModelFixtures.create(ApiUser.class),
                    new Date()
            );
        }
    };
}
