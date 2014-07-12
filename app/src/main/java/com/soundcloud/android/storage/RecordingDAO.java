package com.soundcloud.android.storage;

import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.storage.provider.Content;

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
