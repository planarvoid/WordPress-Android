package com.soundcloud.android.playlists;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(PlaylistItem.class)
public class PlaylistItemBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class).toPropertySet());
        }
    };


}
