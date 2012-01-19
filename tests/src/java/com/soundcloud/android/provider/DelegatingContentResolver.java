package com.soundcloud.android.provider;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(ContentResolver.class)
public class DelegatingContentResolver {
    public final ContentProvider delegate;
    private final List<NotifiedUri> notifiedUris = new ArrayList<NotifiedUri>();

    public DelegatingContentResolver() {
        this(new ScContentProvider());
    }

    public DelegatingContentResolver(ContentProvider delegate) {
        this.delegate = delegate;
        this.delegate.onCreate();
    }

    @Implementation
    public final Uri insert(Uri uri, ContentValues values) {
        return delegate.insert(uri, values);
    }


    @Implementation
    public final Cursor query(Uri uri, String[] projection,
                              String selection, String[] selectionArgs, String sortOrder) {
        return delegate.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Implementation
    public final int delete(Uri url, String where, String[] selectionArgs) {
        return delegate.delete(url, where, selectionArgs);
    }


    @Implementation
    public final int bulkInsert(Uri url, ContentValues[] values) {
        return delegate.bulkInsert(url, values);
    }

    @Implementation
    public final int update(Uri uri, ContentValues values, String where,
                            String[] selectionArgs) {
        return delegate.update(uri, values, where, selectionArgs);
    }

    @Implementation
    public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork) {
        notifiedUris.add(new NotifiedUri(uri, observer, syncToNetwork));
    }

    @Implementation
    public void notifyChange(Uri uri, ContentObserver observer) {
        notifyChange(uri, observer, false);
    }

    public List<NotifiedUri> getNotifiedUris() {
        return notifiedUris;
    }

    public static class NotifiedUri {
        public final Uri uri;
        public final boolean syncToNetwork;
        public final ContentObserver observer;

        NotifiedUri(Uri uri, ContentObserver observer, boolean syncToNetwork) {
            this.uri = uri;
            this.syncToNetwork = syncToNetwork;
            this.observer = observer;
        }
    }
}