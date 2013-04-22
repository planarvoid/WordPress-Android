package com.soundcloud.android.dao;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;

class TrackDAO extends BaseDAO<Track> {
    public TrackDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.TRACKS;
    }
}
