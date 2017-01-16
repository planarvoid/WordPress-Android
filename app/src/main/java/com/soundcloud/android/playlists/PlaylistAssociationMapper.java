package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;
import com.soundcloud.propeller.schema.Column;

import javax.inject.Inject;

@AutoFactory(allowSubclasses = true)
public class PlaylistAssociationMapper extends RxResultMapper<PlaylistAssociation> {

    private final NewPlaylistMapper newPlaylistMapper;
    private Column createdAtField;

    @Inject
    public PlaylistAssociationMapper(@Provided NewPlaylistMapper newPlaylistMapper, Column createdAt) {
        this.newPlaylistMapper = newPlaylistMapper;
        createdAtField = createdAt;
    }

    @Override
    public PlaylistAssociation map(CursorReader cursorReader) {
        final Playlist playlistItem = newPlaylistMapper.map(cursorReader);
        return PlaylistAssociation.create(playlistItem, cursorReader.getDateFromTimestamp(createdAtField));
    }
}
