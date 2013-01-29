package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class AffiliationActivity extends Activity {
    @JsonProperty public User user;

    // for deserialization
    public AffiliationActivity() {
        super();
    }

    public AffiliationActivity(Cursor cursor) {
        super(cursor);
        user = SoundCloudApplication.MODEL_MANAGER.getUserFromActivityCursor(cursor);
    }

    @Override
    public Type getType() {
        return Type.AFFILIATION;
    }

    @Override
    public Track getTrack() {
        return null;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public Playlist getPlaylist() {
        return null;
    }

    @Override
    public void cacheDependencies() {
        this.user = SoundCloudApplication.MODEL_MANAGER.cache(user, ScResource.CacheUpdateMode.MINI);
    }

    @Override
    public ScResource getRefreshableResource() {
        return user;
    }
}
