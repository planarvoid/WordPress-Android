package com.soundcloud.android.dao;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.UriUtils;
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

    @Nullable public LocalCollection fromContentUri(Uri contentUri, boolean createIfNecessary) {
        LocalCollection lc = null;
        final Uri cleanUri = UriUtils.clearQueryParams(contentUri);
        Cursor c = mResolver.query(getContent().uri, null, "uri = ?", new String[]{cleanUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            lc = new LocalCollection(c);
        }
        if (c != null) c.close();

        if (lc == null && createIfNecessary) {
            lc = new LocalCollection(cleanUri);
            create(lc);
        }
        return lc;
    }


    public boolean deleteUri(Uri contentUri) {
        return mResolver.delete(getContent().uri,
                "uri = ?",
                new String[]{UriUtils.clearQueryParams(contentUri).toString()}) == 1;

    }
}
