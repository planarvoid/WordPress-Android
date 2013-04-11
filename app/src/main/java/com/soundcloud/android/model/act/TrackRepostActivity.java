package com.soundcloud.android.model.act;

import android.database.Cursor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.RepostActivity;
import com.soundcloud.android.model.User;
import org.jetbrains.annotations.NotNull;

public class TrackRepostActivity extends TrackActivity implements PlayableHolder, RepostActivity {
    @JsonProperty public User user;

    // for deserialization
    public TrackRepostActivity() {
        super();
    }

    public TrackRepostActivity(Cursor cursor) {
        super(cursor);
        user = User.fromActivityView(cursor);
    }

    @Override
    public Type getType() {
        return Type.TRACK_REPOST;
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
