package com.soundcloud.android.playlists;

import com.soundcloud.java.optional.Optional;

public class PlaylistDetailsHeaderItem extends PlaylistDetailItem{

    private Optional<PlaylistDetailsMetadata> metadataOptional;

    PlaylistDetailsHeaderItem(Optional<PlaylistDetailsMetadata> metadataOptional) {
        super(PlaylistDetailItem.Kind.HeaderItem);
        this.metadataOptional = metadataOptional;
    }


    public Optional<PlaylistDetailsMetadata> getMetadataOptional() {
        return metadataOptional;
    }
}
