package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.User;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

@Blueprint(Playlist.class)
public class PlaylistBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new Playlist(runningId++);
        }
    };

    @Default
    String title = "new playlist " + System.currentTimeMillis();

    @Mapped
    User user;
}
