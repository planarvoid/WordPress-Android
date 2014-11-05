package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiPlaylistRepost;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiPlaylistRepost.class)
public class ApiPlaylistRepostBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiPlaylistRepost(
                    ModelFixtures.create(ApiPlaylist.class),
                    ModelFixtures.create(ApiUser.class),
                    new Date()
            );
        }
    };
}
