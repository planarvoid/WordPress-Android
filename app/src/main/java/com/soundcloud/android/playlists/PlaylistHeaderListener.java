package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;

import android.view.View;

public interface PlaylistHeaderListener {

    void onHeaderPlay();

    void onGoToCreator(View view, Urn creatorUrn);

    void onEditPlaylist();
}
