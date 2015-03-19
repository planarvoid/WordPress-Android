package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.PlaylistTracks;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.functions.Func1;
import rx.util.async.operators.OperatorFromFunctionals;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;

class PlaylistTracksStorage {
    private static final String TRACK_EXISTS_COL_ALIAS = "track_exists_in_playlist";
    private final PropellerDatabase propeller;
    private final DatabaseScheduler scheduler;
    private final DateProvider dateProvider;
    private final AccountOperations accountOperations;

    @Inject
    PlaylistTracksStorage(DatabaseScheduler scheduler, DateProvider dateProvider, AccountOperations accountOperations) {
        this.scheduler = scheduler;
        this.propeller = scheduler.database();
        this.dateProvider = dateProvider;
        this.accountOperations = accountOperations;
    }

    Observable<Urn> createNewPlaylist(final String title, final boolean isPrivate, final Urn firstTrackUrn) {
        final long createdAt = System.currentTimeMillis();
        final long localId = -createdAt;

        return scheduler.scheduleTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.insert(Table.Sounds, getContentValuesForPlaylistsTable(localId, createdAt, title, isPrivate)));
                step(propeller.insert(Table.Posts, getContentValuesForPostsTable(localId, createdAt)));
                step(propeller.insert(Table.PlaylistTracks, getContentValuesForPlaylistTrack(localId, firstTrackUrn)));
            }
        }).map(toPlaylistUrn(localId));
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

    Observable<List<PropertySet>> playlistsForAddingTrack(Urn trackUrn) {
        return scheduler.scheduleQuery(queryPlaylistWithTrackExistStatus(trackUrn))
                .map(new PlaylistWithTrackMapper()).toList();
    }

    private Query queryPlaylistWithTrackExistStatus(Urn trackUrn) {
        return Query.from(Table.SoundView.name())
                .select(
                        field(Table.SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                        field(Table.SoundView.field(TableColumns.SoundView.TITLE)).as(TableColumns.SoundView.TITLE),
                        field(Table.SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TableColumns.SoundView.TRACK_COUNT),
                        count(TableColumns.PlaylistTracks.PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                        exists(trackInPlaylist(trackUrn)).as(TRACK_EXISTS_COL_ALIAS))
                .innerJoin(Table.Posts.name(),
                        on(Table.Posts.field(TableColumns.Posts.TARGET_ID), Table.SoundView.field(TableColumns.SoundView._ID))
                                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), Table.SoundView.field(TableColumns.SoundView._TYPE)))
                .leftJoin(Table.PlaylistTracks.name(), Table.SoundView.field(TableColumns.SoundView._ID), TableColumns.PlaylistTracks.PLAYLIST_ID)
                .whereEq(Table.SoundView.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .groupBy(Table.SoundView.field(TableColumns.SoundView._ID))
                .order(Table.SoundView.field(TableColumns.SoundView.CREATED_AT), Query.ORDER_DESC);
    }

    private Query trackInPlaylist(Urn trackUrn) {
        return Query.from(Table.PlaylistTracks.name())
                .innerJoin(Table.Sounds.name(), filter()
                        .whereEq(PlaylistTracks.PLAYLIST_ID, Table.SoundView.field(SoundView._ID))
                        .whereEq(PlaylistTracks.TRACK_ID, trackUrn.getNumericId())
                        .whereEq(SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST));

    }

    private Func1<TxnResult, Urn> toPlaylistUrn(final long localId) {
        return new Func1<TxnResult, Urn>() {
            @Override
            public Urn call(TxnResult txnResult) {
                if (txnResult.success()) {
                    return Urn.forPlaylist(localId);
                } else {
                    throw txnResult.getFailure();
                }
            }
        };
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

    private ContentValues getContentValuesForPlaylistTrack(long playlistId, Urn firstTrackUrn) {
        return ContentValuesBuilder.values()
                .put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistId)
                .put(TableColumns.PlaylistTracks.TRACK_ID, firstTrackUrn.getNumericId())
                .put(TableColumns.PlaylistTracks.POSITION, 0)
                .get();
    }

    private ContentValues getContentValuesForPlaylistsTable(long localId, long createdAt, String title, boolean isPrivate) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Sounds._ID, localId)
                .put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .put(TableColumns.Sounds.TITLE, title)
                .put(TableColumns.Sounds.SHARING, isPrivate ? Sharing.PRIVATE.value() : Sharing.PUBLIC.value())
                .put(TableColumns.Sounds.CREATED_AT, createdAt)
                .put(TableColumns.Sounds.USER_ID, accountOperations.getLoggedInUserUrn().getNumericId())
                .get();
    }

    private ContentValues getContentValuesForPostsTable(long localId, long createdAt) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Posts.TARGET_ID, localId)
                .put(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .put(TableColumns.Posts.CREATED_AT, createdAt)
                .put(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_POST)
                .get();
    }

    private static final class UpdatedCountMapper extends RxResultMapper<Integer> {
        @Override
        public Integer map(CursorReader cursorReader) {
            return PlaylistMapper.getTrackCount(cursorReader) + 1;
        }
    }

    private static final class PlaylistWithTrackMapper extends PlaylistMapper {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(4);
            propertySet.put(TrackInPlaylistProperty.URN, readSoundUrn(cursorReader));
            propertySet.put(TrackInPlaylistProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
            propertySet.put(TrackInPlaylistProperty.TRACK_COUNT, getTrackCount(cursorReader));
            propertySet.put(TrackInPlaylistProperty.ADDED_TO_URN, cursorReader.getBoolean(TRACK_EXISTS_COL_ALIAS));
            return propertySet;
        }
    }
}
