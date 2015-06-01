package com.soundcloud.android.storage;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.storage.provider.Content;

import android.content.ContentResolver;

import javax.inject.Inject;

/**
 * Table object for users. Do not use outside this package, use {@link LegacyUserStorage} instead.
 */
/* package */ class UserDAO extends BaseDAO<PublicApiUser> {

    @Inject
    public UserDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.USERS;
    }
}
