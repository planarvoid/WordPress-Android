package com.soundcloud.android.dialog;

import com.soundcloud.android.dao.PlaylistStorage;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public abstract class PlaylistDialogFragment extends DialogFragment {
    public static final String KEY_TRACK_ID = "TRACK_ID";

    private PlaylistStorage mPlaylistStorage;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPlaylistStorage = new PlaylistStorage();
    }

    protected PlaylistStorage getPlaylistStorage() {
        return mPlaylistStorage;
    }
}
