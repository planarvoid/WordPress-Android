package com.soundcloud.android.playlists;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.Query;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;

public class PlaylistStorage extends ScheduledOperations {

    private final PropellerDatabase database;

    @Inject
    public PlaylistStorage(PropellerDatabase database) {
        this(database, ScSchedulers.STORAGE_SCHEDULER);
    }

    @VisibleForTesting
    PlaylistStorage(PropellerDatabase database, Scheduler scheduler) {
        super(scheduler);
        this.database = database;
    }


    public Observable<TrackUrn> trackUrns(final PlaylistUrn playlistUrn) {
        Query query = Query.from(Table.PLAYLIST_TRACKS.name)
                .select(TableColumns.PlaylistTracks.TRACK_ID)
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.numericId);
        return schedule(Observable.from(database.query(query)).map(new TrackUrnMapper()));
    }

    private static final class TrackUrnMapper extends RxResultMapper<TrackUrn> {
        @Override
        public TrackUrn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(TableColumns.PlaylistTracks.TRACK_ID));
        }
    }

}
