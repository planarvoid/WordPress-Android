package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ConnectionHolder extends CollectionHolder<Connection> {

    // needed for jackson
    public ConnectionHolder() {}

    public ConnectionHolder(List<Connection> collection) {
        super(collection);
    }

    public int insert(ContentResolver resolver) {
        List<ContentValues> items = new ArrayList<ContentValues>();

        for (Connection c : this) {
            items.add(c.buildContentValues());
        }

        if (!items.isEmpty()) {
            resolver.bulkInsert(Content.ME_CONNECTIONS.uri, items.toArray(new ContentValues[items.size()]));
        }
        return items.size();
    }
}
