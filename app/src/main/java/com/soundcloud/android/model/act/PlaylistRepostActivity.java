package com.soundcloud.android.model.act;

import android.database.Cursor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.RepostActivity;
import com.soundcloud.android.model.User;
import org.jetbrains.annotations.NotNull;

public class PlaylistRepostActivity extends PlaylistActivity implements PlayableHolder, RepostActivity {
    @JsonProperty public User user;

    // for deserialization
    public PlaylistRepostActivity() {
        super();
    }

    public PlaylistRepostActivity(Cursor c) {
        super(c);
        user = User.fromActivityView(c);
    }

    @Override
    public Type getType() {
        return Type.PLAYLIST_REPOST;
    }

    @Override
    public User getUser() {
        return user;
    }

    @NotNull
    @Override
    public User getReposter() {
        return user;
    }
}
