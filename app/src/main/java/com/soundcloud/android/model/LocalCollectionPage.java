package com.soundcloud.android.model;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.api.Request;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

public class LocalCollectionPage {

    public int collection_id;
    public int page_index;
    public int size;
    String etag;

    public LocalCollectionPage(Cursor c){
        collection_id = c.getInt(c.getColumnIndex(TableColumns.CollectionPages.COLLECTION_ID));
        page_index = c.getInt(c.getColumnIndex(TableColumns.CollectionPages.PAGE_INDEX));
        size = c.getInt(c.getColumnIndex(TableColumns.CollectionPages.SIZE));
        etag = c.getString(c.getColumnIndex(TableColumns.CollectionPages.ETAG));
    }

    public LocalCollectionPage(int collection_id, int page_index, int size, String etag){
        this.collection_id = collection_id;
        this.page_index = page_index;
        this.size = size;
        this.etag = etag;
    }

    public Request applyEtag(Request request) {
        if (!TextUtils.isEmpty(etag)) request.ifNoneMatch(etag);
        return request;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.CollectionPages.COLLECTION_ID, collection_id);
        cv.put(TableColumns.CollectionPages.PAGE_INDEX, page_index);
        cv.put(TableColumns.CollectionPages.ETAG, etag);
        cv.put(TableColumns.CollectionPages.SIZE, size);
        return cv;
    }

    public static LocalCollectionPage fromCollectionAndIndex(ContentResolver cr, int collectionId, int pageIndex) {
        LocalCollectionPage lcp = null;
        Cursor c = cr.query(Content.COLLECTION_PAGES.uri, null,
                TableColumns.CollectionPages.COLLECTION_ID + " = ? AND " + TableColumns.CollectionPages.PAGE_INDEX + " = ?",
                new String[]{String.valueOf(collectionId), String.valueOf(pageIndex)}, null);

        if (c != null && c.moveToFirst()) {
            lcp = new LocalCollectionPage(c);
        }
        if (c != null) c.close();
        return lcp;
    }

    @Override
    public String toString() {
        return "LocalCollectionPage{" +
                "collection_id=" + collection_id +
                ", page_index=" + page_index +
                ", size=" + size +
                ", etag='" + etag + '\'' +
                '}';
    }
}
