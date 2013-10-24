package com.soundcloud.android.dialog;

import com.soundcloud.android.dao.PlaylistStorage;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;

import android.os.Bundle;

public abstract class PlaylistDialogFragment extends BaseDialogFragment {
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
