package com.soundcloud.android.api.legacy.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.java.collections.PropertySet;

import android.database.Cursor;

public class TrackLikeActivity extends TrackActivity implements PlayableHolder {
    @JsonProperty public PublicApiUser user;

    // for deserialization
    public TrackLikeActivity() {
        super();
    }

    public TrackLikeActivity(Cursor cursor) {
        super(cursor);
        user = SoundCloudApplication.sModelManager.getCachedUserFromActivityCursor(cursor);
    }

    @Override
    public Type getType() {
        return Type.TRACK_LIKE;
    }

    @Override
    public PublicApiUser getUser() {
        return user;
    }

    @Override
    public void cacheDependencies() {
        super.cacheDependencies();
        this.user = SoundCloudApplication.sModelManager.cache(user, PublicApiResource.CacheUpdateMode.MINI);
    }

    @Override
    public PropertySet toPropertySet() {
        return super.toPropertySet()
                .put(ActivityProperty.TYPE, ActivityProperty.TYPE_LIKE)
                .put(ActivityProperty.SOUND_TITLE, track.getTitle());
    }
}
