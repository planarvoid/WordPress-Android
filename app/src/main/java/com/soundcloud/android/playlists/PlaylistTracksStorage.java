package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.OfflinePlaylistMapper.IS_MARKED_FOR_OFFLINE;
import static com.soundcloud.android.playlists.PlaylistMapper.readSoundUrn;
import static com.soundcloud.android.playlists.PlaylistMapper.readTrackCount;
import static com.soundcloud.android.rx.RxUtils.returning;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.POSITION;
import static com.soundcloud.android.storage.TableColumns.Posts;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.android.storage.TableColumns.Sounds;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies;
import static com.soundcloud.android.storage.TableColumns.Users;
import static com.soundcloud.android.storage.Tables.TrackDownloads;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.ASC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.content.ContentValues;
import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

class PlaylistTracksStorage {
    private static final String IS_TRACK_ALREADY_ADDED = "track_exists_in_playlist";

    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;
    private final AccountOperations accountOperations;

    @Inject
    PlaylistTracksStorage(PropellerRx propellerRx, CurrentDateProvider dateProvider, AccountOperations accountOperations) {
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
        }).map(returning(Urn.forPlaylist(localId)));
    }

    Observable<List<AddTrackToPlaylistItem>> loadAddTrackToPlaylistItems(Urn trackUrn) {
        return propellerRx
                .query(queryPlaylistsWithTrackExistStatus(trackUrn))
                .map(new AddTrackToPlaylistItemMapper())
                .toList();
    }

    Observable<List<PropertySet>> playlistTracks(Urn playlistUrn) {
        return propellerRx.query(getPlaylistTracksQuery(playlistUrn)).map(new PlaylistTrackItemMapper()).toList();
    }

    private Query getPlaylistTracksQuery(Urn playlistUrn) {
        final String fullSoundIdColumn = Table.Sounds.field(Sounds._ID);
        return Query.from(Table.PlaylistTracks.name())
                .select(
                        field(fullSoundIdColumn).as(BaseColumns._ID),
                        Sounds.TITLE,
                        Sounds.USER_ID,
                        Users.USERNAME,
                        Sounds.DURATION,
                        Sounds.PLAYBACK_COUNT,
                        Sounds.LIKES_COUNT,
                        Sounds.SHARING,
                        TrackDownloads.REQUESTED_AT,
                        TrackDownloads.DOWNLOADED_AT,
                        TrackDownloads.UNAVAILABLE_AT,
                        TrackPolicies.SUB_MID_TIER,
                        TrackDownloads.REMOVED_AT,
                        OfflineContent._ID)

                .innerJoin(Table.Sounds.name(), Table.PlaylistTracks.field(PlaylistTracks.TRACK_ID), fullSoundIdColumn)
                .innerJoin(Table.Users.name(), Table.Sounds.field(Sounds.USER_ID), Table.Users.field(Users._ID))
                .leftJoin(TrackDownloads.TABLE.name(), fullSoundIdColumn, TrackDownloads._ID.qualifiedName())
                .leftJoin(Table.TrackPolicies.name(), fullSoundIdColumn, Table.TrackPolicies.field(TrackPolicies.TRACK_ID))
                .leftJoin(OfflineContent.TABLE, offlinePlaylistFilter())

                .whereEq(Table.Sounds.field(Sounds._TYPE), Sounds.TYPE_TRACK)
                .whereEq(Table.PlaylistTracks.field(PlaylistTracks.PLAYLIST_ID), playlistUrn.getNumericId())
                .order(Table.PlaylistTracks.field(POSITION), ASC)
                .whereNull(Table.PlaylistTracks.field(PlaylistTracks.REMOVED_AT));
    }

    private Where offlinePlaylistFilter() {
        return filter()
                .whereEq(OfflineContent._ID, PlaylistTracks.PLAYLIST_ID)
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }

    private Query queryPlaylistsWithTrackExistStatus(Urn trackUrn) {
        return Query.from(Table.SoundView.name())
                .select(
                        field(Table.SoundView.field(SoundView._ID)).as(SoundView._ID),
                        field(Table.SoundView.field(SoundView.TITLE)).as(SoundView.TITLE),
                        field(Table.SoundView.field(SoundView.SHARING)).as(SoundView.SHARING),
                        field(Table.SoundView.field(SoundView.TRACK_COUNT)).as(SoundView.TRACK_COUNT),
                        count(PlaylistTracks.PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                        exists(isTrackInPlaylist(trackUrn)).as(IS_TRACK_ALREADY_ADDED),
                        exists(PlaylistQueries.IS_MARKED_FOR_OFFLINE_QUERY).as(IS_MARKED_FOR_OFFLINE))

                .leftJoin(Table.PlaylistTracks.name(), Table.SoundView.field(SoundView._ID), PlaylistTracks.PLAYLIST_ID)
                .innerJoin(Table.Posts.name(),
                        on(Table.Posts.field(Posts.TARGET_ID), Table.SoundView.field(SoundView._ID))
                                .whereEq(Table.Posts.field(Posts.TARGET_TYPE), Table.SoundView.field(SoundView._TYPE)))

                .whereEq(Table.Posts.field(Posts.TYPE), Posts.TYPE_POST)
                .whereEq(Table.SoundView.field(Sounds._TYPE), Sounds.TYPE_PLAYLIST)
                .groupBy(Table.SoundView.field(SoundView._ID))
                .order(Table.SoundView.field(SoundView.CREATED_AT), Query.Order.DESC);
    }

    private Query isTrackInPlaylist(Urn trackUrn) {
        return Query.from(Table.PlaylistTracks.name())
                .innerJoin(Table.Sounds.name(), filter()
                        .whereEq(PlaylistTracks.PLAYLIST_ID, Table.SoundView.field(SoundView._ID))
                        .whereEq(PlaylistTracks.TRACK_ID, trackUrn.getNumericId())
                        .whereEq(SoundView._TYPE, Sounds.TYPE_PLAYLIST))
                .whereNull(PlaylistTracks.REMOVED_AT);
    }

    private ContentValues getContentValuesForPlaylistTrack(long playlistNumericId, Urn trackUrn, int position) {
        return ContentValuesBuilder.values()
                .put(PlaylistTracks.PLAYLIST_ID, playlistNumericId)
                .put(PlaylistTracks.TRACK_ID, trackUrn.getNumericId())
                .put(PlaylistTracks.POSITION, position)
                .put(PlaylistTracks.ADDED_AT, dateProvider.getDate().getTime())
                .get();
    }

    private ContentValues getContentValuesForPlaylistTrack(long playlistId, Urn firstTrackUrn) {
        return getContentValuesForPlaylistTrack(playlistId, firstTrackUrn, 0);
    }

    private ContentValues getContentValuesForPlaylistsTable(long localId, long createdAt, String title, boolean isPrivate) {
        return ContentValuesBuilder.values()
                .put(Sounds._ID, localId)
                .put(Sounds._TYPE, Sounds.TYPE_PLAYLIST)
                .put(Sounds.TITLE, title)
                .put(Sounds.SHARING, isPrivate ? Sharing.PRIVATE.value() : Sharing.PUBLIC.value())
                .put(Sounds.CREATED_AT, createdAt)
                .put(Sounds.USER_ID, accountOperations.getLoggedInUserUrn().getNumericId())
                .get();
    }

    private ContentValues getContentValuesForPostsTable(long localId, long createdAt) {
        return ContentValuesBuilder.values()
                .put(Posts.TARGET_ID, localId)
                .put(Posts.TARGET_TYPE, Sounds.TYPE_PLAYLIST)
                .put(Posts.CREATED_AT, createdAt)
                .put(Posts.TYPE, Posts.TYPE_POST)
                .get();
    }

    private static final class AddTrackToPlaylistItemMapper extends RxResultMapper<AddTrackToPlaylistItem> {

        @Override
        public AddTrackToPlaylistItem map(CursorReader reader) {
            return new AddTrackToPlaylistItem(
                    readSoundUrn(reader),
                    reader.getString(SoundView.TITLE),
                    readTrackCount(reader),
                    readPrivateFlag(reader),
                    reader.getBoolean(IS_MARKED_FOR_OFFLINE),
                    reader.getBoolean(IS_TRACK_ALREADY_ADDED));
        }

        private boolean readPrivateFlag(CursorReader reader) {
            return Sharing.PRIVATE.name().equalsIgnoreCase(reader.getString(SoundView.SHARING));
        }
    }
}
