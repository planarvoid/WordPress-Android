package com.soundcloud.android.sync.playlists;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;

class ReplacePlaylistTracksCommand extends StoreCommand<List<Urn>> {

    private Urn playlistUrn;

    @Inject
    public ReplacePlaylistTracksCommand(PropellerDatabase database) {
        super(database);
    }

    public ReplacePlaylistTracksCommand with(Urn playlistUrn) {
        this.playlistUrn = playlistUrn;
        return this;
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(Table.PlaylistTracks,
                                      filter().whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID,
                                                       playlistUrn.getNumericId())));
                for (int i = 0; i < input.size(); i++) {
                    step(propeller.upsert(Table.PlaylistTracks, buildPlaylistTrackContentValues(input.get(i), i)));
                }
            }
        });
    }

    private ContentValues buildPlaylistTrackContentValues(Urn trackUrn, int position) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId());
        contentValues.put(TableColumns.PlaylistTracks.TRACK_ID, trackUrn.getNumericId());
        contentValues.put(TableColumns.PlaylistTracks.POSITION, position);
        return contentValues;
    }
}
