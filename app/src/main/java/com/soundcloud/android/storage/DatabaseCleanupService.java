package com.soundcloud.android.storage;

import static com.soundcloud.android.storage.StorageModule.DB_CLEANUP_HELPERS;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseCleanupService extends IntentService {

    static final String TAG = "DatabaseCleanupService";
    private static final int BATCH_SIZE = 500;

    @Inject PropellerDatabase propellerDatabase;
    @Inject @Named(DB_CLEANUP_HELPERS) List<CleanupHelper> cleanupHelpers;

    public DatabaseCleanupService() {
        super(TAG);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    public DatabaseCleanupService(PropellerDatabase propellerDatabase,
                                  List<CleanupHelper> cleanupHelpers) {
        super(TAG);
        this.propellerDatabase = propellerDatabase;
        this.cleanupHelpers = cleanupHelpers;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Set<Urn> usersToKeep = new HashSet<>();
        Set<Urn> tracksToKeep = new HashSet<>();
        Set<Urn> playlistsToKeep = new HashSet<>();

        for (CleanupHelper cleanupHelper : cleanupHelpers) {
            usersToKeep.addAll(cleanupHelper.usersToKeep());
            tracksToKeep.addAll(cleanupHelper.tracksToKeep());
            playlistsToKeep.addAll(cleanupHelper.playlistsToKeep());
        }

        tracksToKeep.addAll(getTracksForPlaylists(playlistsToKeep));
        usersToKeep.addAll(getUsersForTrack(tracksToKeep));
        usersToKeep.addAll(getUsersForPlaylist(playlistsToKeep));

        propellerDatabase.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propellerDatabase) {

                // playlists
                for (List<Urn> urnBatch : Lists.partition(getPlaylistsToDelete(playlistsToKeep), BATCH_SIZE)) {
                    step(propellerDatabase.delete(Table.PlaylistTracks, filter().whereIn(TableColumns.PlaylistTracks.PLAYLIST_ID, Lists.transform(urnBatch, Urn::getNumericId))));
                    step(propellerDatabase.delete(Tables.Sounds.TABLE, filter().whereEq(Tables.Sounds._TYPE, TYPE_PLAYLIST).whereIn(Tables.Sounds._ID, Lists.transform(urnBatch, Urn::getNumericId))));
                }

                // tracks
                List<Urn> allTracksToDelete = getTracksToDelete(tracksToKeep);
                for (List<Urn> urnBatch : Lists.partition(allTracksToDelete, BATCH_SIZE)) {
                    step(propellerDatabase.delete(Tables.Sounds.TABLE, filter().whereEq(Tables.Sounds._TYPE, TYPE_TRACK).whereIn(Tables.Sounds._ID, Lists.transform(urnBatch, Urn::getNumericId))));
                    step(propellerDatabase.delete(Tables.TrackPolicies.TABLE, filter().whereIn(Tables.TrackPolicies.TRACK_ID, Lists.transform(urnBatch, Urn::getNumericId))));
                }

                // users
                List<Urn> allUsersToDelete = getUsersToDelete(usersToKeep);
                for (List<Urn> urnBatch : Lists.partition(allUsersToDelete, BATCH_SIZE)) {
                    step(propellerDatabase.delete(Tables.Users.TABLE, filter().whereIn(Tables.Users._ID, Lists.transform(urnBatch, Urn::getNumericId))));
                }
            }
        });

    }

    @NonNull
    private List<Urn> getUsersToDelete(Set<Urn> usersToKeep) {
        List<Urn> allUsersToDelete = getAllUserFromDB();
        allUsersToDelete.removeAll(usersToKeep);
        return allUsersToDelete;
    }

    @NonNull
    private List<Urn> getTracksToDelete(Set<Urn> tracksToKeep) {
        List<Urn> allTracksToDelete = getAllTracksFromDB();
        allTracksToDelete.removeAll(tracksToKeep);
        return allTracksToDelete;
    }

    @NonNull
    private List<Urn> getPlaylistsToDelete(Set<Urn> playlistsToKeep) {
        List<Urn> allPlaylistToDelete = getAllPlaylistsFromDB();
        allPlaylistToDelete.removeAll(playlistsToKeep);
        return allPlaylistToDelete;
    }

    private List<Urn> getAllUserFromDB() {
        QueryResult result = propellerDatabase.query(Query.from(Tables.Users.TABLE).select(Tables.Users._ID));
        return result.toList(cursorReader -> Urn.forUser(cursorReader.getLong(Tables.Users._ID)));
    }

    private List<Urn> getAllTracksFromDB() {
        QueryResult result = propellerDatabase.query(Query.from(Tables.Sounds.TABLE).select(Tables.Sounds._ID).whereEq(Tables.Sounds._TYPE, TYPE_TRACK));
        return result.toList(cursorReader -> Urn.forTrack(cursorReader.getLong(Tables.Sounds._ID)));
    }

    private List<Urn> getAllPlaylistsFromDB() {
        QueryResult result = propellerDatabase.query(Query.from(Tables.Sounds.TABLE).select(Tables.Sounds._ID).whereEq(Tables.Sounds._TYPE, TYPE_PLAYLIST));
        return result.toList(cursorReader -> Urn.forPlaylist(cursorReader.getLong(Tables.Sounds._ID)));
    }

    private Set<Urn> getTracksForPlaylists(Set<Urn> playlistsToKeep) {
        Set<Urn> tracksFromPlaylists = new HashSet<>();
        for (List<Urn> batch : Lists.partition(Lists.newArrayList(playlistsToKeep), BATCH_SIZE)) {
            List<Urn> tracks = propellerDatabase.query(Query.from(Table.PlaylistTracks)
                                                            .select(TableColumns.PlaylistTracks.TRACK_ID)
                                                            .whereIn(TableColumns.PlaylistTracks.PLAYLIST_ID, Lists.transform(batch, Urn::getNumericId))
            ).toList(cursorReader -> Urn.forTrack(cursorReader.getLong(TableColumns.PlaylistTracks.TRACK_ID)));
            tracksFromPlaylists.addAll(tracks);
        }
        return tracksFromPlaylists;
    }

    private Set<Urn> getUsersForTrack(Set<Urn> tracksToKeep) {
        Set<Urn> usersFromTracks = new HashSet<>();
        for (List<Urn> batch : Lists.partition(Lists.newArrayList(tracksToKeep), BATCH_SIZE)) {
            List<Urn> users = propellerDatabase.query(Query.from(Tables.Sounds.TABLE)
                                                           .select(Tables.Sounds.USER_ID)
                                                           .whereEq(_TYPE, TYPE_TRACK)
                                                           .whereIn(Tables.Sounds._ID, Lists.transform(batch, Urn::getNumericId))
            ).toList(cursorReader -> Urn.forUser(cursorReader.getLong(Tables.Sounds.USER_ID)));
            usersFromTracks.addAll(users);
        }
        return usersFromTracks;
    }

    private Set<Urn> getUsersForPlaylist(Set<Urn> playlistsToKeep) {
        Set<Urn> usersFromPlaylists = new HashSet<>();
        for (List<Urn> batch : Lists.partition(Lists.newArrayList(playlistsToKeep), BATCH_SIZE)) {
            List<Urn> users = propellerDatabase.query(Query.from(Tables.Sounds.TABLE)
                                                           .select(Tables.Sounds.USER_ID)
                                                           .whereEq(_TYPE, TYPE_PLAYLIST)
                                                           .whereIn(Tables.Sounds._ID, Lists.transform(batch, Urn::getNumericId))
            ).toList(cursorReader -> Urn.forUser(cursorReader.getLong(Tables.Sounds.USER_ID)));
            usersFromPlaylists.addAll(users);
        }
        return usersFromPlaylists;
    }

}
