package com.soundcloud.android.playlists;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class EditPlaylistCommand
        extends WriteStorageCommand<EditPlaylistCommand.EditPlaylistCommandParams, WriteResult, Integer> {

    private int updatedTrackCount;

    private final CurrentDateProvider dateProvider;

    @Inject
    protected EditPlaylistCommand(PropellerDatabase propeller, CurrentDateProvider dateProvider) {
        super(propeller);
        this.dateProvider = dateProvider;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final EditPlaylistCommandParams input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                Where whereClause = Filter.filter()
                                          .whereEq(Tables.Sounds._ID, input.playlistUrn.getNumericId())
                                          .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_PLAYLIST);
                final ChangeResult step = step(propeller.update(Tables.Sounds.TABLE, getContentValuesForPlaylistsTable(input),
                                                                whereClause));

                if (step.getNumRowsAffected() > 0) {
                    setMissingTracksAsRemoved(propeller);
                    setNewTrackPositions(propeller);

                    updatedTrackCount = input.trackList.size();
                }
            }

            private void setMissingTracksAsRemoved(PropellerDatabase propeller) {
                Where whereClause = Filter.filter()
                                          .whereEq(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID),
                                                   input.playlistUrn.getNumericId())
                                          .whereNotIn(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID),
                                                      Urns.toIds(input.trackList))
                                          .whereNull(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.REMOVED_AT));
                step(propeller.update(Table.PlaylistTracks, getRemovedAtContentValues(), whereClause));
            }

            private void setNewTrackPositions(PropellerDatabase propeller) {
                Set<Urn> toUpdate = tracksToUpdate(propeller, input);

                for (int i = 0; i < input.trackList.size(); i++) {
                    Urn trackUrn = input.trackList.get(i);
                    Urn playlistUrn = input.playlistUrn;
                    if (toUpdate.contains(trackUrn)) {
                        step(propeller.update(Table.PlaylistTracks, buildPlaylistTrackUpdateContentValues(i),
                                              Filter.filter().whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID,playlistUrn.getNumericId())
                                                    .whereEq(TableColumns.PlaylistTracks.TRACK_ID,trackUrn.getNumericId())));
                    } else {
                        ContentValues contentValues = buildPlaylistTrackAdditionContentValues(playlistUrn.getNumericId(), trackUrn.getNumericId(), i);
                        step(propeller.insert(Table.PlaylistTracks, contentValues));
                    }


                }
            }
        });
    }

    private Set<Urn> tracksToUpdate(PropellerDatabase propeller, EditPlaylistCommandParams input) {
        QueryResult query = propeller.query(String.valueOf(Query.from(Table.PlaylistTracks)
                                                                .select(TableColumns.PlaylistTracks.TRACK_ID)
                                                                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID,
                                                                         input.playlistUrn.getNumericId())

                                                                .whereIn(TableColumns.PlaylistTracks.TRACK_ID, Lists.transform(input.trackList, Urn::getNumericId))));
        Set<Urn> toUpdate = new HashSet<>(query.getResultCount());
        for (CursorReader cursorReader : query) {
            toUpdate.add(Urn.forTrack(cursorReader.getLong(TableColumns.PlaylistTracks.TRACK_ID)));
        }
        return toUpdate;
    }

    private ContentValues getRemovedAtContentValues() {
        return ContentValuesBuilder.values()
                                   .put(TableColumns.PlaylistTracks.REMOVED_AT, dateProvider.getCurrentTime())
                                   .put(TableColumns.PlaylistTracks.POSITION, Consts.NOT_SET)
                                   .get();
    }

    private ContentValues getContentValuesForPlaylistsTable(EditPlaylistCommandParams input) {
        return ContentValuesBuilder.values()
                                   .put(Tables.Sounds.TITLE, input.playlistTitle)
                                   .put(Tables.Sounds.SHARING,
                                        input.isPrivate ? Sharing.PRIVATE.value() : Sharing.PUBLIC.value())
                                   .put(Tables.Sounds.MODIFIED_AT, dateProvider.getCurrentTime())
                                   .get();
    }

    private ContentValues buildPlaylistTrackUpdateContentValues(int position) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TableColumns.PlaylistTracks.POSITION, position);
        contentValues.putNull(TableColumns.PlaylistTracks.REMOVED_AT);
        return contentValues;
    }

    private ContentValues buildPlaylistTrackAdditionContentValues(long playlistId, long trackId, int position) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistId);
        contentValues.put(TableColumns.PlaylistTracks.TRACK_ID, trackId);
        contentValues.put(TableColumns.PlaylistTracks.POSITION, position);
        contentValues.put(TableColumns.PlaylistTracks.ADDED_AT, dateProvider.getCurrentTime());
        return contentValues;
    }

    @Override
    protected Integer transform(WriteResult result) {
        return updatedTrackCount;
    }

    static final class EditPlaylistCommandParams {
        final Urn playlistUrn;
        final String playlistTitle;
        final boolean isPrivate;
        final List<Urn> trackList;

        EditPlaylistCommandParams(final Urn playlistUrn, String playlistTitle, boolean isPrivate, List<Urn> trackList) {
            this.playlistUrn = playlistUrn;
            this.playlistTitle = playlistTitle;
            this.isPrivate = isPrivate;
            this.trackList = trackList;
        }
    }
}
