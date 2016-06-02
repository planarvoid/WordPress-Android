package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class PlaylistCollectionItem extends CollectionItem {

    static PlaylistCollectionItem create(PlaylistItem playlistItem) {
        return new AutoValue_PlaylistCollectionItem(CollectionItem.TYPE_PLAYLIST_ITEM, playlistItem);
    }

    abstract PlaylistItem getPlaylistItem();

    @Override
    public boolean isSingleSpan() {
        return true;
    }

    @Override
    public ListItem update(PropertySet sourceSet) {
        getPlaylistItem().update(sourceSet);
        return this;
    }

    @Override
    public Urn getUrn() {
        return getPlaylistItem().getUrn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return getPlaylistItem().getImageUrlTemplate();
    }

}
