package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.ColumnFunctions.count;

import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.content.ContentValues;

import javax.inject.Inject;

class AddTrackToPlaylistCommand extends WriteStorageCommand<AddTrackToPlaylistCommand.AddTrackToPlaylistParams, WriteResult, Integer> {

    private int updatedTrackCount;

    private final DateProvider dateProvider;

    @Inject
    AddTrackToPlaylistCommand(PropellerDatabase propeller, CurrentDateProvider dateProvider) {
        super(propeller);
        this.dateProvider = dateProvider;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final AddTrackToPlaylistParams params) {
        final int currentTrackCount = getCurrentTrackCount(propeller, params.playlistUrn);
        updatedTrackCount = currentTrackCount + 1;
        return propeller.insert(Table.PlaylistTracks,
                getContentValues(params.playlistUrn.getNumericId(), params.trackUrn, currentTrackCount));
    }

    private int getCurrentTrackCount(PropellerDatabase propeller, Urn playlistUrn) {
        return propeller.query(Query.from(Table.SoundView.name())
                .select(
                        SoundView.TRACK_COUNT,
                        count(TableColumns.PlaylistTracks.PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT))
                .whereEq(SoundView._ID, playlistUrn.getNumericId())
                .whereEq(SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .leftJoin(Table.PlaylistTracks.name(), SoundView._ID, TableColumns.PlaylistTracks.PLAYLIST_ID))
                .first(new TrackCountMapper());
    }

    private ContentValues getContentValues(long playlistId, Urn trackUrn, int position) {
        return ContentValuesBuilder.values()
                .put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistId)
                .put(TableColumns.PlaylistTracks.TRACK_ID, trackUrn.getNumericId())
                .put(TableColumns.PlaylistTracks.POSITION, position)
                .put(TableColumns.PlaylistTracks.ADDED_AT, dateProvider.getCurrentDate().getTime())
                .get();
    }

    @Override
    protected Integer transform(WriteResult result) {
        return updatedTrackCount;
    }

    static final class AddTrackToPlaylistParams {
        final Urn playlistUrn;
        final Urn trackUrn;

        AddTrackToPlaylistParams(final Urn playlistUrn, final Urn trackUrn) {
            this.playlistUrn = playlistUrn;
            this.trackUrn = trackUrn;
        }
    }

    private static final class TrackCountMapper extends RxResultMapper<Integer> {
        @Override
        public Integer map(CursorReader cursorReader) {
            return PlaylistMapper.readTrackCount(cursorReader);
        }
    }
}

