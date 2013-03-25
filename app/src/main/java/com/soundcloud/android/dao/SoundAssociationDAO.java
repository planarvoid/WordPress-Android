package com.soundcloud.android.dao;

import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.provider.Content;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;

import java.util.ArrayList;
import java.util.List;

class SoundAssociationDAO extends BaseDAO<SoundAssociation> {
    public SoundAssociationDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public List<SoundAssociation> queryAll() {
        List<SoundAssociation> result = new ArrayList<SoundAssociation>();
        result.addAll(queryAllByUri(Content.ME_LIKES.uri)); // liked tracks & playlists
        result.addAll(queryAllByUri(Content.ME_SOUNDS.uri)); // own tracks, own playlists, or reposts
        return result;
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
