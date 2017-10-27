package com.soundcloud.android.api.model;

import com.soundcloud.android.api.model.stream.ApiStreamPlaylistPost;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiStreamPlaylistPost.class)
public class ApiPlaylistPostBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiStreamPlaylistPost(
                    PlaylistFixtures.apiPlaylist(),
                    new Date()
            );
        }
    };
}
