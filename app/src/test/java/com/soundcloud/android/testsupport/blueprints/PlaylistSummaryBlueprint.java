package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.legacy.model.PlayableStats;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Blueprint(ApiPlaylist.class)
public class PlaylistSummaryBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiPlaylist("soundcloud:playlists:" + runningId++);
        }
    };

    @Default
    String title = "playlist " + System.currentTimeMillis();

    @Mapped
    ApiUser user;

    @Default
    List<String> tags = Arrays.asList("tag1", "tag2", "tag3");

    @Default(force = true)
    Integer trackCount = 5;

    @Default
    String artworkUrl = "http://assets.soundcloud.com/1";

    @Default
    Date createdAt = new Date();

    @Default
    Sharing sharing = Sharing.PUBLIC;

    @Default
    PlayableStats stats = new PlayableStats() {
        @Override
        public int getRepostsCount() {
            return 5;
        }

        @Override
        public int getLikesCount() {
            return 10;
        }
    };
}
