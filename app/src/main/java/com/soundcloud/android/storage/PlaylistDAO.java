package com.soundcloud.android.storage;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.storage.provider.Content;

import android.content.ContentResolver;

import javax.inject.Inject;

/**
 * Table object for playlists. Do not use outside this package, use {@link PlaylistStorage} instead.
 */
/* package */ class PlaylistDAO extends BaseDAO<Playlist> {

    @Inject
    public PlaylistDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.PLAYLISTS;
    }
}
