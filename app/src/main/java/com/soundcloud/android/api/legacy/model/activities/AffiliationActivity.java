package com.soundcloud.android.api.legacy.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.java.collections.PropertySet;

import android.database.Cursor;

public class AffiliationActivity extends Activity {

    private PublicApiUser user;

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
    public PublicApiUser getUser() {
        return user;
    }

    @JsonProperty
    public void setUser(PublicApiUser user) {
        this.user = user;
    }

    @Override
    public void cacheDependencies() {
        this.user = SoundCloudApplication.sModelManager.cache(user, PublicApiResource.CacheUpdateMode.MINI);
    }

    @Override
    public Refreshable getRefreshableResource() {
        return user;
    }

    @Override
    public boolean isIncomplete() {
        return user == null || user.isIncomplete();
    }

    @Override
    public PropertySet toPropertySet() {
        return super.toPropertySet()
                .put(ActivityProperty.TYPE, ActivityProperty.TYPE_FOLLOWER)
                .put(ActivityProperty.USER_NAME, user.getUsername());
    }
}
