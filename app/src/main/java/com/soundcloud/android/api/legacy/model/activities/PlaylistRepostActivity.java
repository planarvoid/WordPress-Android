package com.soundcloud.android.api.legacy.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Repost;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.NotNull;

import android.database.Cursor;

public class PlaylistRepostActivity extends PlaylistActivity implements PlayableHolder, Repost {
    @JsonProperty public PublicApiUser user;

    // for deserialization
    public PlaylistRepostActivity() {
        super();
    }

    public PlaylistRepostActivity(Cursor c) {
        super(c);
        user = SoundCloudApplication.sModelManager.getCachedUserFromActivityCursor(c);
    }

    @Override
    public Type getType() {
        return Type.PLAYLIST_REPOST;
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

    @NotNull
    @Override
    public PublicApiUser getReposter() {
        return user;
    }

    @Override
    public PropertySet toPropertySet() {
        return super.toPropertySet()
                .put(ActivityProperty.TYPE, ActivityProperty.TYPE_REPOST)
                .put(ActivityProperty.SOUND_TITLE, playlist.getTitle());
    }
}
