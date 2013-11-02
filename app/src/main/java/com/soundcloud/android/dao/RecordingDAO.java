package com.soundcloud.android.dao;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;

/**
 * Table object for recording models. Do not use outside this package, use {@link RecordingStorage} instead.
 */
/* package */ class RecordingDAO extends BaseDAO<Recording> {
    public RecordingDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.RECORDINGS;
    }
}
