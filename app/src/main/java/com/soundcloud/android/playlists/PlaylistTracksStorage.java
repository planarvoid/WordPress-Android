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
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;

class PlaylistTracksStorage {
    private static final String TRACK_EXISTS_COL_ALIAS = "track_exists_in_playlist";
    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;
    private final AccountOperations accountOperations;

    @Inject
    PlaylistTracksStorage(PropellerRx propellerRx, DateProvider dateProvider, AccountOperations accountOperations) {
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
        this.accountOperations = accountOperations;
    }

    Observable<Urn> createNewPlaylist(final String title, final boolean isPrivate, final Urn firstTrackUrn) {
        final long createdAt = System.currentTimeMillis();
        final long localId = -createdAt;

        return propellerRx.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.insert(Table.Sounds, getContentValuesForPlaylistsTable(localId, createdAt, title, isPrivate)));
                step(propeller.insert(Table.Posts, getContentValuesForPostsTable(localId, createdAt)));
                step(propeller.insert(Table.PlaylistTracks, getContentValuesForPlaylistTrack(localId, firstTrackUrn)));
            }
        }).map(toPlaylistUrn(localId));
    }

    Observable<List<PropertySet>> playlistsForAddingTrack(Urn trackUrn) {
        return propellerRx.query(queryPlaylistWithTrackExistStatus(trackUrn))
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
                .leftJoin(Table.PlaylistTracks.name(), Table.SoundView.field(TableColumns.SoundView._ID), TableColumns.PlaylistTracks.PLAYLIST_ID)
                .innerJoin(Table.Posts.name(),
                        on(Table.Posts.field(TableColumns.Posts.TARGET_ID), Table.SoundView.field(TableColumns.SoundView._ID))
                                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), Table.SoundView.field(TableColumns.SoundView._TYPE)))
                .whereEq(Table.Posts.field(TableColumns.Posts.TYPE), TableColumns.Posts.TYPE_POST)
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
                return Urn.forPlaylist(localId);
            }
        };
    }

    private ContentValues getContentValuesForPlaylistTrack(long playlistNumericId, Urn trackUrn, int position) {
        return ContentValuesBuilder.values()
                .put(PlaylistTracks.PLAYLIST_ID, playlistNumericId)
                .put(PlaylistTracks.TRACK_ID, trackUrn.getNumericId())
                .put(PlaylistTracks.POSITION, position)
                .put(PlaylistTracks.ADDED_AT, dateProvider.getCurrentDate().getTime())
                .get();
    }

    private ContentValues getContentValuesForPlaylistTrack(long playlistId, Urn firstTrackUrn) {
        return getContentValuesForPlaylistTrack(playlistId, firstTrackUrn, 0);
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
