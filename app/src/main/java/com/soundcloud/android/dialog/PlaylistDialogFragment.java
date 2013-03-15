package com.soundcloud.android.dialog;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.dao.PlaylistDAO;

public abstract class PlaylistDialogFragment extends SherlockDialogFragment {
    public static final String KEY_TRACK_ID = "TRACK_ID";

    protected PlaylistDAO mPlaylistDAO;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPlaylistDAO = new PlaylistDAO(getActivity().getContentResolver());
    }
}
