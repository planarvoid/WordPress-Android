package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.OfflinePlaylistMapper.IS_MARKED_FOR_OFFLINE;
import static com.soundcloud.android.rx.RxUtils.returning;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks;
import static com.soundcloud.android.storage.TableColumns.Posts;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.android.storage.TableColumns.Sounds;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;

class PlaylistTracksStorage {
    private static final String IS_TRACK_ALREADY_ADDED = "track_exists_in_playlist";

    private final LoadPlaylistTracksCommand loadPlaylistTracksCommand;
    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;
    private final AccountOperations accountOperations;

    @Inject
    PlaylistTracksStorage(PropellerRx propellerRx,
                          LoadPlaylistTracksCommand loadPlaylistTracksCommand,
                          CurrentDateProvider dateProvider,
                          AccountOperations accountOperations) {
        this.loadPlaylistTracksCommand = loadPlaylistTracksCommand;
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
        this.accountOperations = accountOperations;
    }

    private Query queryPlaylistsWithTrackExistStatus(Urn trackUrn) {
        return Query.from(Table.SoundView.name())
                    .select(
                            field(Table.SoundView.field(SoundView._ID)).as(SoundView._ID),
                            field(Table.SoundView.field(SoundView.TITLE)).as(SoundView.TITLE),
                            field(Table.SoundView.field(SoundView.ARTWORK_URL)).as(SoundView.ARTWORK_URL),
                            field(Table.SoundView.field(SoundView.SHARING)).as(SoundView.SHARING),
                            field(Table.SoundView.field(SoundView.TRACK_COUNT)).as(SoundView.TRACK_COUNT),
                            count(PlaylistTracks.PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                            exists(isTrackInPlaylist(trackUrn)).as(IS_TRACK_ALREADY_ADDED),
                            exists(PlaylistQueries.IS_MARKED_FOR_OFFLINE_QUERY).as(
                                    IS_MARKED_FOR_OFFLINE))

                    .leftJoin(Table.PlaylistTracks.name(),
                              Table.SoundView.field(SoundView._ID),
                              PlaylistTracks.PLAYLIST_ID)
                    .innerJoin(Table.Posts.name(),
                               on(Table.Posts.field(Posts.TARGET_ID),
                                  Table.SoundView.field(SoundView._ID))
                                       .whereEq(Table.Posts.field(Posts.TARGET_TYPE),
                                                Table.SoundView.field(SoundView._TYPE)))

                    .whereEq(Table.Posts.field(Posts.TYPE), Posts.TYPE_POST)
                    .whereEq(Table.SoundView.field(Sounds._TYPE), Sounds.TYPE_PLAYLIST)
                    .groupBy(Table.SoundView.field(SoundView._ID))
                    .order(Table.SoundView.field(SoundView.CREATED_AT), Query.Order.DESC);
    }

    private Query isTrackInPlaylist(Urn trackUrn) {
        return Query.from(Table.PlaylistTracks.name())
                    .innerJoin(Table.Sounds.name(), filter()
                            .whereEq(PlaylistTracks.PLAYLIST_ID,
                                     Table.SoundView.field(SoundView._ID))
                            .whereEq(PlaylistTracks.TRACK_ID, trackUrn.getNumericId())
                            .whereEq(SoundView._TYPE, Sounds.TYPE_PLAYLIST))
                    .whereNull(Table.PlaylistTracks.field(PlaylistTracks.REMOVED_AT));
    }

    private ContentValues getContentValuesForPlaylistTrack(long playlistNumericId,
                                                           Urn trackUrn,
                                                           int position) {
        return ContentValuesBuilder.values()
                                   .put(PlaylistTracks.PLAYLIST_ID, playlistNumericId)
                                   .put(PlaylistTracks.TRACK_ID, trackUrn.getNumericId())
                                   .put(PlaylistTracks.POSITION, position)
                                   .put(PlaylistTracks.ADDED_AT,
                                        dateProvider.getCurrentDate().getTime())
                                   .get();
    }

    private ContentValues getContentValuesForPlaylistTrack(Urn playlist, Urn firstTrackUrn) {
        return getContentValuesForPlaylistTrack(playlist.getNumericId(), firstTrackUrn, 0);
    }

    private ContentValues getContentValuesForPlaylistsTable(Urn playlist,
                                                            long createdAt,
                                                            String title,
                                                            boolean isPrivate) {
        return ContentValuesBuilder.values()
                                   .put(Sounds._ID, playlist.getNumericId())
                                   .put(Sounds._TYPE, Sounds.TYPE_PLAYLIST)
                                   .put(Sounds.TITLE, title)
                                   .put(Sounds.SHARING,
                                        isPrivate ?
                                        Sharing.PRIVATE.value() :
                                        Sharing.PUBLIC.value())
                                   .put(Sounds.CREATED_AT, createdAt)
                                   .put(Sounds.USER_ID,
                                        accountOperations.getLoggedInUserUrn().getNumericId())
                                   .put(Sounds.SET_TYPE, Strings.EMPTY)
                                   .put(Sounds.RELEASE_DATE, Strings.EMPTY)
                                   .get();
    }

    private ContentValues getContentValuesForPostsTable(Urn playlist, long createdAt) {
        return ContentValuesBuilder.values()
                                   .put(Posts.TARGET_ID, playlist.getNumericId())
                                   .put(Posts.TARGET_TYPE, Sounds.TYPE_PLAYLIST)
                                   .put(Posts.CREATED_AT, createdAt)
                                   .put(Posts.TYPE, Posts.TYPE_POST)
                                   .get();
    }

    Observable<Urn> createNewPlaylist(final String title,
                                      final boolean isPrivate,
                                      final Urn firstTrackUrn) {
        final long createdAt = dateProvider.getCurrentTime();

        final Urn playlist = Urn.newLocalPlaylist();
        return propellerRx.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.insert(Table.Sounds,
                                      getContentValuesForPlaylistsTable(playlist,
                                                                        createdAt,
                                                                        title,
                                                                        isPrivate)));
                step(propeller.insert(Table.Posts,
                                      getContentValuesForPostsTable(playlist, createdAt)));
                step(propeller.insert(Table.PlaylistTracks,
                                      getContentValuesForPlaylistTrack(playlist, firstTrackUrn)));
            }
        }).map(returning(playlist));
    }

    Observable<List<AddTrackToPlaylistItem>> loadAddTrackToPlaylistItems(Urn trackUrn) {
        return propellerRx
                .query(queryPlaylistsWithTrackExistStatus(trackUrn))
                .map(new AddTrackToPlaylistItemMapper())
                .toList();
    }

    Observable<List<PropertySet>> playlistTracks(Urn playlistUrn) {
        return loadPlaylistTracksCommand.toObservable(playlistUrn);
    }

    private static final class AddTrackToPlaylistItemMapper
            extends RxResultMapper<AddTrackToPlaylistItem> {

        @Override
        public AddTrackToPlaylistItem map(CursorReader reader) {
            return new AddTrackToPlaylistItem(
                    Urn.forPlaylist(reader.getLong(TableColumns.SoundView._ID)),
                    reader.getString(SoundView.TITLE),
                    Math.max(reader.getInt(PlaylistMapper.LOCAL_TRACK_COUNT),
                             reader.getInt(TableColumns.SoundView.TRACK_COUNT)),
                    readPrivateFlag(reader),
                    reader.getBoolean(IS_MARKED_FOR_OFFLINE),
                    reader.getBoolean(IS_TRACK_ALREADY_ADDED));
        }

        private boolean readPrivateFlag(CursorReader reader) {
            return Sharing.PRIVATE.name().equalsIgnoreCase(reader.getString(SoundView.SHARING));
        }
    }
}
