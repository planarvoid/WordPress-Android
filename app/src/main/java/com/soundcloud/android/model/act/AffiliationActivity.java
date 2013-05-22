package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Refreshable;
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
        user = SoundCloudApplication.MODEL_MANAGER.getCachedUserFromActivityCursor(cursor);
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
    public void cacheDependencies() {
        this.user = SoundCloudApplication.MODEL_MANAGER.cache(user, ScResource.CacheUpdateMode.MINI);
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
