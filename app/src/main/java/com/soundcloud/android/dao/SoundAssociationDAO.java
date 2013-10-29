package com.soundcloud.android.dao;

import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Table object for sound associations. Do not use outside this package, use {@link SoundAssociationStorage} instead.
 */
/* package */ class SoundAssociationDAO extends BaseDAO<SoundAssociation> {
    public SoundAssociationDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    public static SoundAssociationDAO forContent(final Content content, final ContentResolver contentResolver) {
        return new SoundAssociationDAO(contentResolver) {
            @Override
            public Content getContent() {
                return content;
            }
        };
    }

    @Override
    @NotNull
    public List<SoundAssociation> queryAll() {
        List<SoundAssociation> result = new ArrayList<SoundAssociation>();
        result.addAll(queryAllByUri(Content.ME_LIKES.uri)); // liked tracks & playlists
        result.addAll(queryAllByUri(Content.ME_SOUNDS.uri)); // own tracks, own playlists, or reposts
        return result;
    }

    @Override
    public boolean delete(SoundAssociation resource) {
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
    public Class<SoundAssociation> getModelClass() {
        return SoundAssociation.class;
    }
}
