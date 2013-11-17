package com.soundcloud.android.storage;

import com.soundcloud.android.model.Connection;
import com.soundcloud.android.storage.provider.Content;

import android.content.ContentResolver;

public class ConnectionDAO extends BaseDAO<Connection> {
    public ConnectionDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.ME_CONNECTIONS;
    }
}
