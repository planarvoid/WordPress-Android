package com.soundcloud.android.dao;

import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

class UserAssociationDAO extends BaseDAO<UserAssociation> {
    public UserAssociationDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    public static UserAssociationDAO forContent(final Content content, final ContentResolver contentResolver) {
        return new UserAssociationDAO(contentResolver) {
            @Override
            public Content getContent() {
                return content;
            }
        };
    }

    @Override
    public List<UserAssociation> queryAll() {
        List<UserAssociation> result = new ArrayList<UserAssociation>();
        result.addAll(queryAllByUri(Content.ME_FOLLOWINGS.uri));
        result.addAll(queryAllByUri(Content.ME_FOLLOWERS.uri));
        return result;
    }

    @Override
    public boolean delete(UserAssociation resource) {
        String where = DBHelper.UserAssociations.TARGET_ID + "=? AND " +
                DBHelper.UserAssociations.ASSOCIATION_TYPE + "=?";

        return delete(getContent().uri,
                where,
                String.valueOf(resource.getItemId()),
                String.valueOf(resource.associationType));
    }

    @Nullable
    public UserAssociation query(long targetId) {
        String where = DBHelper.UserAssociationView._ID + " = ? AND " +
                DBHelper.UserAssociationView.USER_ASSOCIATION_TYPE + " = ?";

        Cursor cursor = mResolver.query(getContent().uri, null, where,
                new String[]{String.valueOf(targetId), String.valueOf(getContent().collectionType)}, null);
        if (cursor == null) return null;

        try {
            if (cursor.moveToFirst()) {
                return objFromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public boolean update(UserAssociation userAssociation) {
        final String where = DBHelper.UserAssociations.TARGET_ID + " = ? AND " +
                DBHelper.UserAssociations.ASSOCIATION_TYPE + " = ?";
        final String[] args = {String.valueOf(userAssociation.getItemId()),
                String.valueOf(userAssociation.associationType)};
        return mResolver.update(getContent().uri, userAssociation.buildContentValues(), where, args) == 1;
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
