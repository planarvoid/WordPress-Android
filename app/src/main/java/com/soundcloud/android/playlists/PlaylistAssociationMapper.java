package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;

@AutoFactory(allowSubclasses = true)
public class PlaylistAssociationMapper extends RxResultMapper<PlaylistAssociation> {

    private final NewPlaylistMapper newPlaylistMapper;
    private String createdAtField;

    @Inject
    public PlaylistAssociationMapper(@Provided NewPlaylistMapper newPlaylistMapper, String createdAt) {
        this.newPlaylistMapper = newPlaylistMapper;
        createdAtField = createdAt;
    }

    @Override
    public PlaylistAssociation map(CursorReader cursorReader) {
        final PlaylistItem playlistItem = newPlaylistMapper.map(cursorReader);
        return PlaylistAssociation.create(playlistItem, cursorReader.getDateFromTimestamp(createdAtField));
    }
}
