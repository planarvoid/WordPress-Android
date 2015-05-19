package com.soundcloud.android.playlists;

import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;

class RemoveTrackFromPlaylistCommand extends WriteStorageCommand<RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams, WriteResult, Integer> {

    private int updatedTrackCount;

    @Inject
    RemoveTrackFromPlaylistCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final RemoveTrackFromPlaylistParams params) {
        final List<Urn> playlistTracks = propeller.query(getPlaylistTracks(params.playlistUrn)).toList(new GetPlaylistTrackUrnsMapper());
        playlistTracks.remove(params.trackUrn);

        updatedTrackCount = playlistTracks.size();

        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(Table.PlaylistTracks, Filter.filter()
                        .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, params.playlistUrn.getNumericId())
                        .whereNull(TableColumns.PlaylistTracks.REMOVED_AT)));

                for (int i = 0; i < playlistTracks.size(); i++) {
                    step(propeller.upsert(Table.PlaylistTracks, buildPlaylistTrackContentValues(params.playlistUrn, playlistTracks.get(i), i)));
                }
                step(propeller.insert(Table.PlaylistTracks, getInsertTrackPendingRemovalParams(params.playlistUrn, params.trackUrn)));
            }
        });
    }

    private ContentValues buildPlaylistTrackContentValues(Urn playlistUrn, Urn trackUrn, int position){
        ContentValues contentValues = new ContentValues();
        contentValues.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId());
        contentValues.put(TableColumns.PlaylistTracks.TRACK_ID, trackUrn.getNumericId());
        contentValues.put(TableColumns.PlaylistTracks.POSITION, position);
        return contentValues;
    }

    private ContentValues getInsertTrackPendingRemovalParams(Urn playlistUrn, Urn trackForRemoval) {
        return ContentValuesBuilder.values()
                .put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId())
                .put(TableColumns.PlaylistTracks.TRACK_ID, trackForRemoval.getNumericId())
                .put(TableColumns.PlaylistTracks.REMOVED_AT, System.currentTimeMillis())
                .get();
    }

    private Query getPlaylistTracks(Urn playlistUrn) {
        return Query.from(Table.PlaylistTracks.name())
                .select(TableColumns.PlaylistTracks.TRACK_ID)
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId())
                .whereNull(TableColumns.PlaylistTracks.REMOVED_AT)
                .order(TableColumns.PlaylistTracks.POSITION, Query.ORDER_ASC);
    }

    @Override
    protected Integer transform(WriteResult result) {
        return updatedTrackCount;
    }

    static final class RemoveTrackFromPlaylistParams {
        final Urn playlistUrn;
        final Urn trackUrn;

        RemoveTrackFromPlaylistParams(final Urn playlistUrn, final Urn trackUrn) {
            this.playlistUrn = playlistUrn;
            this.trackUrn = trackUrn;
        }
    }

    private static final class GetPlaylistTrackUrnsMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(TableColumns.PlaylistTracks.TRACK_ID));
        }
    }
}

