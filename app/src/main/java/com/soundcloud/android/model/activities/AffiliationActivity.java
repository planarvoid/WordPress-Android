package com.soundcloud.android.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.behavior.Refreshable;

import android.database.Cursor;

import java.util.Date;

public class AffiliationActivity extends Activity {
    @JsonProperty public User user;

    // for deserialization
    public AffiliationActivity() {
        super();
    }

    public AffiliationActivity(Cursor cursor) {
        super(cursor);
        user = SoundCloudApplication.sModelManager.getCachedUserFromActivityCursor(cursor);
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

    @SuppressWarnings("unused") // ModelCitizen needs this
    public void setUser(User user) {
        this.user = user;
    }

    @SuppressWarnings("unused") // ModelCitizen needs this
    public Date getCreatedAt() {
        return created_at;
    }

    @SuppressWarnings("unused") // ModelCitizen needs this
    public void setCreatedAt(Date date) {
        this.created_at = date;
    }

    @Override
    public void cacheDependencies() {
        this.user = SoundCloudApplication.sModelManager.cache(user, ScResource.CacheUpdateMode.MINI);
    }

    @Override
    public Refreshable getRefreshableResource() {
        return user;
    }

    @Override
    public boolean isIncomplete() {
        return user == null || user.isIncomplete();
    }


}
