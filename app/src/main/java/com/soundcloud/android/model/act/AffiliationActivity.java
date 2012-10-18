package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Playlist;
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

    @Override @JsonIgnore
    public void setCachedTrack(Track track) {
        // nop
    }

    @Override @JsonIgnore
    public void setCachedUser(User user) {
        this.user = user;
    }

}
