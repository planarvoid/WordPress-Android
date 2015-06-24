package com.soundcloud.android.api.legacy.model;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Deprecated
@Blueprint(PublicApiPlaylist.class)
public class PublicApiPlaylistBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new PublicApiPlaylist(runningId++);
        }
    };

    @Default
    String title = "new playlist " + System.currentTimeMillis();

    @Default
    Date createdAt = new Date();

    @Mapped
    PublicApiUser user;

    @Default
    String permalinkUrl = "permalink-url";
}
