package com.soundcloud.android.dao;

import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;

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
        String where = DBHelper.CollectionItems.ITEM_ID + "=? AND " +
                DBHelper.CollectionItems.RESOURCE_TYPE + "=? AND " +
                DBHelper.CollectionItems.COLLECTION_TYPE + "=?";

        return delete(getContent().uri,
                where,
                String.valueOf(resource.getItemId()),
                String.valueOf(resource.getResourceType()),
                String.valueOf(resource.associationType));
    }

    @Override
    public Content getContent() {
        return Content.COLLECTION_ITEMS;
    }

    @NotNull
    @Override
    public Class<UserAssociation> getModelClass() {
        return UserAssociation.class;
    }
}
