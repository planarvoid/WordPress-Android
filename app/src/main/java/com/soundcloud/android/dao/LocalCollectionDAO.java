package com.soundcloud.android.dao;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class LocalCollectionDAO extends BaseDAO<LocalCollection> {
    public LocalCollectionDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override public Content getContent() {
        return Content.COLLECTIONS;
    }

    public @Nullable LocalCollection fromContentUri(Uri contentUri, boolean createIfNecessary) {
        LocalCollection lc = null;
        Cursor c = mResolver.query(getContent().uri, null, "uri = ?", new String[]{contentUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            lc = new LocalCollection(c);
        }
        if (c != null) c.close();

        if (lc == null && createIfNecessary){
            lc = new LocalCollection(0, contentUri, -1, -1, 0, -1, null);
            create(lc);
        }
        return lc;
    }


    public boolean deleteUri(Uri contentUri) {
        return mResolver.delete(getContent().uri,
                "uri = ?",
                new String[] { contentUri.toString() }) == 1;

    }
}
