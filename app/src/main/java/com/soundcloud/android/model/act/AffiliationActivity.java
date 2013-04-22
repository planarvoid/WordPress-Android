package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScResource;
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
        user = User.fromActivityView(cursor);
    }

    @Override
    public Type getType() {
        return Type.AFFILIATION;
    }

    @Override
    public Playable getPlayable() {
        return null;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public ScResource getRefreshableResource() {
        return user;
    }

    @Override
    public boolean isIncomplete() {
        return user == null || user.isIncomplete();
    }


}
