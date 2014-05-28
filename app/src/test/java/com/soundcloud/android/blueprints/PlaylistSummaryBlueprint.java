package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.PlayableStats;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.Sharing;
import com.soundcloud.android.model.UserSummary;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Blueprint(PlaylistSummary.class)
public class PlaylistSummaryBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new PlaylistSummary("soundcloud:playlists:" + runningId++);
        }
    };

    @Default
    String title = "playlist " + System.currentTimeMillis();

    @Mapped
    UserSummary user;

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
