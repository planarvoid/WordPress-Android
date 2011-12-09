package com.soundcloud.android.model;

import android.content.ContentResolver;
import android.database.Cursor;
import android.text.TextUtils;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.api.Request;

public class LocalCollectionPage {

    public int collection_id;
    public int page_index;
    public int size;
    String nextHref;
    String etag;

    public LocalCollectionPage(Cursor c){
        collection_id = c.getInt(c.getColumnIndex(DBHelper.CollectionPages.COLLECTION_ID));
        page_index = c.getInt(c.getColumnIndex(DBHelper.CollectionPages.PAGE_INDEX));
        size = c.getInt(c.getColumnIndex(DBHelper.CollectionPages.SIZE));
        nextHref = c.getString(c.getColumnIndex(DBHelper.CollectionPages.NEXT_HREF));
        etag = c.getString(c.getColumnIndex(DBHelper.CollectionPages.ETAG));
    }

    public void applyEtag(Request request) {
        if (!TextUtils.isEmpty(etag)) request.ifNoneMatch(etag);
    }

    public static LocalCollectionPage fromCollectionAndIndex(ContentResolver cr, int collectionId, int pageIndex) {
        LocalCollectionPage lcp = null;
        Cursor c = cr.query(Content.COLLECTION_PAGES.uri, null,
                DBHelper.CollectionPages.COLLECTION_ID + " = ? AND " + DBHelper.CollectionPages.PAGE_INDEX + " = ?",
                new String[]{String.valueOf(collectionId), String.valueOf(pageIndex)}, null);

        if (c != null && c.moveToFirst()) {
            lcp = new LocalCollectionPage(c);
        }
        if (c != null) c.close();
        return lcp;
    }
}
