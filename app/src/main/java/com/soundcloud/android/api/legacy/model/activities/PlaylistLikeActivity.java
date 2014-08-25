package com.soundcloud.android.api.legacy.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.propeller.PropertySet;

import android.database.Cursor;

public class PlaylistLikeActivity extends PlaylistActivity implements PlayableHolder {
    @JsonProperty public PublicApiUser user;

    public PlaylistLikeActivity() {
        super();
    }

    public PlaylistLikeActivity(Cursor cursor) {
        super(cursor);
        user = SoundCloudApplication.sModelManager.getCachedUserFromActivityCursor(cursor);
    }

    @Override
    public Type getType() {
        return Type.PLAYLIST_LIKE;
    }

    @Override
    public PublicApiUser getUser() {
        return user;
    }

    @Override
    public PropertySet toPropertySet() {
        return super.toPropertySet()
                .put(ActivityProperty.TYPE, ActivityProperty.TYPE_LIKE)
                .put(ActivityProperty.SOUND_TITLE, playlist.getTitle());
    }
}
