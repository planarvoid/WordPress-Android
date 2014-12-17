package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.likes.ApiPlaylistLike;
import com.soundcloud.android.model.Urn;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiPlaylistLike.class)
public class ApiPlaylistLikeBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiPlaylistLike(Urn.forPlaylist(123L), new Date());
        }
    };
}
