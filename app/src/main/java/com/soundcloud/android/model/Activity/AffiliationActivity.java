package com.soundcloud.android.model.Activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DBHelper;

import android.content.ContentValues;
import android.database.Cursor;

public class AffiliationActivity extends Activity {
    public User user;

    // for deserialization
    public AffiliationActivity() {
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

    @Override @JsonIgnore
    public void setCachedTrack(Track track) {
        // nop
    }

    @Override @JsonIgnore
    public void setCachedUser(User user) {
        this.user = user;
    }
}
