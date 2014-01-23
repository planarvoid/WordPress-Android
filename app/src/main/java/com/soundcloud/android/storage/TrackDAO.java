package com.soundcloud.android.storage;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.storage.provider.Content;

import android.content.ContentResolver;

import javax.inject.Inject;

/**
 * Table object for tracks. Do not use outside this package, use {@link TrackStorage} instead.
 */
/* package */ class TrackDAO extends BaseDAO<Track> {

    @Inject
    public TrackDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.TRACKS;
    }
}
