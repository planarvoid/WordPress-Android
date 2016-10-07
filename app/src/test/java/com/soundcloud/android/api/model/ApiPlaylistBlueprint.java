package com.soundcloud.android.api.model;

import com.soundcloud.android.api.legacy.model.PlayableStats;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.strings.Strings;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.callback.AfterCreateCallback;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Blueprint(ApiPlaylist.class)
public class ApiPlaylistBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiPlaylist(Urn.forPlaylist(runningId++));
        }
    };

    @Mapped
    ApiUser user;

    @Default
    List<String> tags = Arrays.asList("tag1", "tag2", "tag3");

    @Default(force = true)
    Integer trackCount = 2;

    @Default
    Date createdAt = new Date();

    @Default
    Sharing sharing = Sharing.PUBLIC;

    @Default
    String permalinkUrl = "http://permalink";

    @Default
    String setType = Strings.EMPTY;

    @Default
    String releaseDate = Strings.EMPTY;

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

    // avoid the setter problem where getters and setters are typed differently
    // https://github.com/mguymon/model-citizen/issues/20
    AfterCreateCallback<ApiPlaylist> afterCreate = new AfterCreateCallback<ApiPlaylist>() {
        @Override
        public ApiPlaylist afterCreate(ApiPlaylist model) {
            model.setArtworkUrlTemplate("https://i1.sndcdn.com/artworks-000151307749-v2r7oy-{size}.jpg");
            model.setTitle("Playlist " + runningId);
            return model;
        }
    };
}
