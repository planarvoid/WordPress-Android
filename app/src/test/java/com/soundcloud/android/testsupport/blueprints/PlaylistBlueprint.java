package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(PublicApiPlaylist.class)
public class PlaylistBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new PublicApiPlaylist(runningId++);
        }
    };

    @Default
    String title = "new playlist " + System.currentTimeMillis();

    @Mapped
    PublicApiUser user;
}
