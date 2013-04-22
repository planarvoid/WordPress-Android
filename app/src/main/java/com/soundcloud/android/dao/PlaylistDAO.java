package com.soundcloud.android.dao;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;

class PlaylistDAO extends BaseDAO<Playlist> {
    public PlaylistDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.PLAYLISTS;
    }
}
