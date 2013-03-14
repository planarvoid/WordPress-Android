package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.Nullable;

public class LocalCollectionDAO extends BaseDAO<LocalCollection> {
    public LocalCollectionDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override public Content getContent() {
        return Content.COLLECTIONS;
    }

    public @Nullable LocalCollection insertLocalCollection(
                                          Uri contentUri,
                                          int syncState,
                                          long lastSyncAttempt,
                                          long lastSyncSuccess,
                                          int size,
                                          String extra) {
        // create if not there
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.URI, contentUri.toString());
        if (lastSyncAttempt != -1) cv.put(DBHelper.Collections.LAST_SYNC_ATTEMPT, lastSyncAttempt);
        if (lastSyncSuccess != -1) cv.put(DBHelper.Collections.LAST_SYNC, lastSyncSuccess);
        if (size != -1)        cv.put(DBHelper.Collections.SIZE, size);
        cv.put(DBHelper.Collections.SYNC_STATE, syncState);
        cv.put(DBHelper.Collections.EXTRA, extra);

        long id = create(cv);
        return new LocalCollection((int) id, contentUri, lastSyncAttempt,lastSyncSuccess, syncState, size, extra);
    }

    public @Nullable LocalCollection fromContent(Content content, boolean createIfNecessary) {
        return fromContentUri(content.uri, createIfNecessary);
    }

    public @Nullable LocalCollection fromContentUri(Uri contentUri, boolean createIfNecessary) {
        LocalCollection lc = null;
        Cursor c = mResolver.query(getContent().uri, null, "uri = ?", new String[]{contentUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            lc = new LocalCollection(c);
        }
        if (c != null) c.close();

        if (lc == null && createIfNecessary){
            lc = insertLocalCollection(contentUri, 0, -1, -1, -1, null);
        }
        return lc;
    }


    public boolean deleteUri(Uri contentUri) {
        return mResolver.delete(getContent().uri,
                "uri = ?",
                new String[] { contentUri.toString() }) == 1;

    }
}
