package com.soundcloud.android.dialog;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.dao.PlaylistStorage;

public abstract class PlaylistDialogFragment extends SherlockDialogFragment {
    public static final String KEY_TRACK_ID = "TRACK_ID";

    private PlaylistStorage mPlaylistStorage;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPlaylistStorage = new PlaylistStorage(getActivity());
    }

    protected PlaylistStorage getPlaylistStorage() {
        return mPlaylistStorage;
    }
}
