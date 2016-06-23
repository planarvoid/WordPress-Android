package com.soundcloud.android.playlists;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Filter;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;

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
                final ChangeResult step = step(propeller.update(Table.Sounds, getContentValuesForPlaylistsTable(input),
                                                                Filter.filter()
                                                                      .whereEq(Table.Sounds.id,
                                                                               input.playlistUrn.getNumericId())
                                                                      .whereEq(Table.Sounds.type,
                                                                               TableColumns.Sounds.TYPE_PLAYLIST)));

                if (step.getNumRowsAffected() > 0) {
                    setMissingTracksAsRemoved(propeller);
                    setNewTrackPositions(propeller);

                    updatedTrackCount = input.trackList.size();
                }
            }

            private void setMissingTracksAsRemoved(PropellerDatabase propeller) {
                step(propeller.update(Table.PlaylistTracks, getRemovedAtContentValues(), Filter.filter()
                                                                                               .whereEq(Table.PlaylistTracks
                                                                                                                .field(TableColumns.PlaylistTracks.PLAYLIST_ID),
                                                                                                        input.playlistUrn
                                                                                                                .getNumericId())
                                                                                               .whereNotIn(Table.PlaylistTracks
                                                                                                                   .field(TableColumns.PlaylistTracks.TRACK_ID),
                                                                                                           Urns.toIds(
                                                                                                                   input.trackList))
                                                                                               .whereNull(Table.PlaylistTracks
                                                                                                                  .field(TableColumns.PlaylistTracks.REMOVED_AT))));
            }

            private void setNewTrackPositions(PropellerDatabase propeller) {
                for (int i = 0; i < input.trackList.size(); i++) {
                    step(propeller.update(Table.PlaylistTracks, buildPlaylistTrackContentValues(i),
                                          Filter.filter()
                                                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID,
                                                         input.playlistUrn.getNumericId())
                                                .whereEq(TableColumns.PlaylistTracks.TRACK_ID,
                                                         input.trackList.get(i).getNumericId())));

                }
            }
        });
    }

    private ContentValues getRemovedAtContentValues() {
        return ContentValuesBuilder.values()
                                   .put(TableColumns.PlaylistTracks.REMOVED_AT, dateProvider.getCurrentTime())
                                   .put(TableColumns.PlaylistTracks.POSITION, Consts.NOT_SET)
                                   .get();
    }

    private ContentValues getContentValuesForPlaylistsTable(EditPlaylistCommandParams input) {
        return ContentValuesBuilder.values()
                                   .put(TableColumns.Sounds.TITLE, input.playlistTitle)
                                   .put(TableColumns.Sounds.SHARING,
                                        input.isPrivate ? Sharing.PRIVATE.value() : Sharing.PUBLIC.value())
                                   .put(TableColumns.Sounds.MODIFIED_AT, dateProvider.getCurrentTime())
                                   .get();
    }

    private ContentValues buildPlaylistTrackContentValues(int position) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TableColumns.PlaylistTracks.POSITION, position);
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
