package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;

public interface PlaylistHeaderListener {

    void onHeaderPlay();

    void onGoToCreator(Urn creatorUrn);

    void onEditPlaylist();
}
