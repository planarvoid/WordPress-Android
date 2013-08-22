package com.soundcloud.android.dao;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;

class RecordingDAO extends BaseDAO<Recording> {
    public RecordingDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.RECORDINGS;
    }
}
