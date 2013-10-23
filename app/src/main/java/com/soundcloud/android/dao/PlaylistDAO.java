package com.soundcloud.android.dao;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;

/**
 * Table object for playlists. Do not use outside this package, use {@link PlaylistStorage} instead.
 */
/* package */ class PlaylistDAO extends BaseDAO<Playlist> {
    public PlaylistDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.PLAYLISTS;
    }
}
