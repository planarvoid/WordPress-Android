package com.soundcloud.android.storage;

import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.storage.provider.Content;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;

/**
 * Table object for user associations. Do not use outside this package, use {@link UserAssociationStorage} instead.
 */
/* package */ class UserAssociationDAO extends BaseDAO<UserAssociation> {
    public UserAssociationDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    protected static UserAssociationDAO forContent(final Content content, final ContentResolver contentResolver) {
        return new UserAssociationDAO(contentResolver) {
            @Override
            public Content getContent() {
                return content;
            }
        };
    }

    @Override
    public boolean delete(UserAssociation resource) {
        String where = TableColumns.UserAssociations.TARGET_ID + "=? AND " +
                TableColumns.UserAssociations.ASSOCIATION_TYPE + "=?";

        return delete(getContent().uri,
                where,
                String.valueOf(resource.getItemId()),
                String.valueOf(resource.associationType));
    }

    public boolean update(UserAssociation userAssociation) {
        final String where = TableColumns.UserAssociations.TARGET_ID + " = ? AND " +
                TableColumns.UserAssociations.ASSOCIATION_TYPE + " = ?";
        final String[] args = {String.valueOf(userAssociation.getItemId()),
                String.valueOf(userAssociation.associationType)};
        return resolver.update(getContent().uri, userAssociation.buildContentValues(), where, args) == 1;
    }

    @Override
    public Content getContent() {
        return Content.USER_ASSOCIATIONS;
    }

    @NotNull
    @Override
    public Class<UserAssociation> getModelClass() {
        return UserAssociation.class;
    }
}
