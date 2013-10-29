package com.soundcloud.android.dao;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;

/**
 * Table object for tracks. Do not use outside this package, use {@link TrackStorage} instead.
 */
/* package */ class TrackDAO extends BaseDAO<Track> {
    public TrackDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.TRACKS;
    }
}
