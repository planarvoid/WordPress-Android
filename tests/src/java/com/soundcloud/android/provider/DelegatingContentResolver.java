package com.soundcloud.android.provider;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(ContentResolver.class)
public class DelegatingContentResolver {
    public final ContentProvider delegate;

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
}