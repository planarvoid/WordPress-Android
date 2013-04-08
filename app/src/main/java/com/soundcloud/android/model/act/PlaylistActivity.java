package com.soundcloud.android.model.act;

import android.database.Cursor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SharingNote;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DBHelper;

public class PlaylistActivity extends Activity implements PlayableHolder {
    @JsonProperty public Playlist playlist;
    @JsonProperty public SharingNote sharingNote;

    // for deserialization
    public PlaylistActivity() {
        super();
    }

    public PlaylistActivity(Cursor c) {
        super(c);
        playlist = SoundCloudApplication.MODEL_MANAGER.getCachedPlaylistFromCursor(c, DBHelper.ActivityView.SOUND_ID);
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
        this.playlist = SoundCloudApplication.MODEL_MANAGER.cache(playlist);
    }

    @Override
    public ScResource getRefreshableResource() {
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
