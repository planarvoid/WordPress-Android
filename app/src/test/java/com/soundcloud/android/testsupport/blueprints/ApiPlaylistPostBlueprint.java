package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.stream.ApiPlaylistPost;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(ApiPlaylistPost.class)
public class ApiPlaylistPostBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiPlaylistPost(
                    ModelFixtures.create(ApiPlaylist.class)
            );
        }
    };
}
