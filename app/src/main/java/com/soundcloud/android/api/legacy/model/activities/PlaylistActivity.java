package com.soundcloud.android.api.legacy.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.storage.TableColumns;

import android.database.Cursor;

public abstract class PlaylistActivity extends Activity implements PlayableHolder {
    @JsonProperty public PublicApiPlaylist playlist;

    // for deserialization
    public PlaylistActivity() {
        super();
    }

    public PlaylistActivity(Cursor c) {
        super(c);
        playlist = SoundCloudApplication.sModelManager.getCachedPlaylistFromCursor(c, TableColumns.ActivityView.SOUND_ID);
    }

    @Override
    public PublicApiUser getUser() {
        return playlist == null ? null : playlist.user;
    }

    @Override
    public void cacheDependencies() {
        this.playlist = SoundCloudApplication.sModelManager.cache(playlist);
    }

    @Override
    public Refreshable getRefreshableResource() {
        return playlist;
    }

    @Override
    public Playable getPlayable() {
        return playlist;
    }

    @Override
    public boolean isIncomplete() {
        return playlist == null || playlist.isIncomplete();
    }
}
