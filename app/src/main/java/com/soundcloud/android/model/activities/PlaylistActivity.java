package com.soundcloud.android.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SharingNote;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.behavior.Refreshable;
import com.soundcloud.android.storage.TableColumns;

import android.database.Cursor;

public class PlaylistActivity extends Activity implements PlayableHolder {
    @JsonProperty public Playlist playlist;
    @JsonProperty public SharingNote sharingNote;

    // for deserialization
    public PlaylistActivity() {
        super();
    }

    public PlaylistActivity(Cursor c) {
        super(c);
        playlist = SoundCloudApplication.sModelManager.getCachedPlaylistFromCursor(c, TableColumns.ActivityView.SOUND_ID);
    }

    @Override
    public Type getType() {
        return Type.PLAYLIST;
    }

    @Override
    public User getUser() {
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
