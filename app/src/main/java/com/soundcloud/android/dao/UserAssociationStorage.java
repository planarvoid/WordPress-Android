package com.soundcloud.android.dao;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.ScContentProvider;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.List;

/**
 * Use this storage facade to persist information about user-to-user relations to the database.
 * These relations currently are: followers and followings.
 *
 * @see com.soundcloud.android.model.UserAssociation.Type
 */
public class UserAssociationStorage {
    private final ContentResolver mResolver;

    public UserAssociationStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
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
