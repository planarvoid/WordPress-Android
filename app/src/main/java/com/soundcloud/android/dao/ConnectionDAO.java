package com.soundcloud.android.dao;

import android.content.ContentResolver;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.provider.Content;

public class ConnectionDAO extends BaseDAO<Connection> {
    public ConnectionDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.ME_CONNECTIONS;
    }
}
