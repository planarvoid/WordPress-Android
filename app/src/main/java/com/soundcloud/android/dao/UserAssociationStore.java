package com.soundcloud.android.dao;

import com.soundcloud.android.provider.ScContentProvider;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.util.List;

public class UserAssociationStore {
    private final ContentResolver mResolver;

    public UserAssociationStore(Context context) {
        mResolver = context.getContentResolver();
    }

    public List<Long> getStoredIds(Uri uri) {
        return ResolverHelper.idCursorToList(
            mResolver.query(
            uri.buildUpon().appendQueryParameter(ScContentProvider.Parameter.IDS_ONLY, "1").build(),
            null,
            null,
            null,
            null)
        );
    }
}
