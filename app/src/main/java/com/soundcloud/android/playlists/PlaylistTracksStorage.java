package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.PlaylistTracks;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.ColumnFunctions.count;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.util.async.operators.OperatorFromFunctionals;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.concurrent.Callable;

class PlaylistTracksStorage {
    private final PropellerDatabase propeller;
    private final DateProvider dateProvider;

    @Inject
    PlaylistTracksStorage(PropellerDatabase propeller, DateProvider dateProvider) {
        this.dateProvider = dateProvider;
        this.propeller = propeller;
    }

    Observable<PropertySet> addTrackToPlaylist(final Urn playlistUrn, final Urn trackUrn) {
        return Observable.create(OperatorFromFunctionals.fromCallable(new Callable<PropertySet>() {
            @Override
            public PropertySet call() throws Exception {
                final int trackCount = getUpdatedTracksCount(playlistUrn);
                final InsertResult insert = propeller.insert(Table.PlaylistTracks,
                        getContentValues(playlistUrn.getNumericId(), trackUrn, trackCount - 1));

                if (insert.success()) {
                    return PropertySet.from(
                            PlaylistProperty.URN.bind(playlistUrn),
                            PlaylistProperty.TRACK_COUNT.bind(trackCount));
                } else {
                    throw insert.getFailure();
                }
            }
        }));
    }

    private int getUpdatedTracksCount(Urn playlistUrn) {
        return propeller.query(Query.from(Table.SoundView.name())
                .select(
                        SoundView.TRACK_COUNT,
                        count(PlaylistTracks.PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT))
                .whereEq(SoundView._ID, playlistUrn.getNumericId())
                .whereEq(SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .leftJoin(Table.PlaylistTracks.name(), SoundView._ID, PlaylistTracks.PLAYLIST_ID))
                .toList(new UpdatedCountMapper()).get(0);
    }

    private ContentValues getContentValues(long playlistId, Urn trackUrn, int position) {
        return ContentValuesBuilder.values()
                .put(PlaylistTracks.PLAYLIST_ID, playlistId)
                .put(PlaylistTracks.TRACK_ID, trackUrn.getNumericId())
                .put(PlaylistTracks.POSITION, position)
                .put(PlaylistTracks.ADDED_AT, dateProvider.getCurrentDate().getTime())
                .get();
    }

    private static final class UpdatedCountMapper extends RxResultMapper<Integer> {
        @Override
        public Integer map(CursorReader cursorReader) {
            return PlaylistMapper.getTrackCount(cursorReader) + 1;
        }
    }

}
