package com.soundcloud.android.api.legacy.model.activities;

import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;

import android.database.Cursor;

public class PlaylistSharingActivity extends PlaylistActivity implements PlayableHolder {

    public PlaylistSharingActivity() {
        super();
    }

    public PlaylistSharingActivity(Cursor c) {
        super(c);
    }

    @Override
    public Type getType() {
        return Type.PLAYLIST_SHARING;
    }
}
