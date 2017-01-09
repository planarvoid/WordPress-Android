package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.android.collection.playhistory.PlayHistoryRecord.CONTEXT_ARTIST;
import static com.soundcloud.android.collection.playhistory.PlayHistoryRecord.CONTEXT_ARTIST_STATION;
import static com.soundcloud.android.collection.playhistory.PlayHistoryRecord.CONTEXT_PLAYLIST;
import static com.soundcloud.android.collection.playhistory.PlayHistoryRecord.CONTEXT_TRACK_STATION;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.FetchAndStoreStationsCommand;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchUsersCommand;
import com.soundcloud.rx.eventbus.EventBus;

import android.util.SparseArray;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

class RecentlyPlayedSyncer implements Callable<Boolean> {

    private final PushRecentlyPlayedCommand pushRecentlyPlayedCommand;
    private final FetchRecentlyPlayedCommand fetchRecentlyPlayedCommand;
    private final FetchPlaylistsCommand fetchPlaylistsCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final FetchUsersCommand fetchUsersCommand;
    private final StoreUsersCommand storeUsersCommand;
    private final EventBus eventBus;
    private final OptimizeRecentlyPlayedCommand optimizeRecentlyPlayedCommand;
    private final RecentlyPlayedStorage recentlyPlayedStorage;
    private final FetchAndStoreStationsCommand fetchAndStoreStationsCommand;

    @Inject
    RecentlyPlayedSyncer(RecentlyPlayedStorage recentlyPlayedStorage,
                         FetchRecentlyPlayedCommand fetchRecentlyPlayedCommand,
                         PushRecentlyPlayedCommand pushRecentlyPlayedCommand,
                         FetchPlaylistsCommand fetchPlaylistsCommand,
                         StorePlaylistsCommand storePlaylistsCommand,
                         FetchUsersCommand fetchUsersCommand,
                         StoreUsersCommand storeUsersCommand,
                         EventBus eventBus,
                         OptimizeRecentlyPlayedCommand optimizeRecentlyPlayedCommand,
                         FetchAndStoreStationsCommand fetchAndStoreStationsCommand) {
        this.recentlyPlayedStorage = recentlyPlayedStorage;
        this.pushRecentlyPlayedCommand = pushRecentlyPlayedCommand;
        this.fetchRecentlyPlayedCommand = fetchRecentlyPlayedCommand;
        this.fetchPlaylistsCommand = fetchPlaylistsCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.fetchUsersCommand = fetchUsersCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.eventBus = eventBus;
        this.optimizeRecentlyPlayedCommand = optimizeRecentlyPlayedCommand;
        this.fetchAndStoreStationsCommand = fetchAndStoreStationsCommand;
    }

    @Override
    public Boolean call() throws Exception {
        boolean hasChanges = updateLocalStorage();
        pushUnSyncedPlayHistory();
        return hasChanges;
    }

    private void pushUnSyncedPlayHistory() {
        optimizeRecentlyPlayedCommand.call(RecentlyPlayedOperations.MAX_RECENTLY_PLAYED);
        pushRecentlyPlayedCommand.call();
    }

    private boolean updateLocalStorage() throws Exception {
        Collection<PlayHistoryRecord> remote = fetchRecentlyPlayedCommand.call();
        Collection<PlayHistoryRecord> current = recentlyPlayedStorage.loadSyncedRecentlyPlayed();

        boolean hasChanges = !current.equals(remote);

        if (hasChanges) {
            addPlayHistory(current, remote);
            removePlayHistory(current, remote);
            eventBus.publish(EventQueue.PLAY_HISTORY, PlayHistoryEvent.updated());
        }

        return hasChanges;
    }

    private void addPlayHistory(Collection<PlayHistoryRecord> current,
                                Collection<PlayHistoryRecord> remote) throws Exception {
        List<PlayHistoryRecord> insertRecords = new ArrayList<>(remote);
        insertRecords.removeAll(current);

        if (!insertRecords.isEmpty()) {
            preloadNewContexts(insertRecords);
            recentlyPlayedStorage.insertRecentlyPlayed(insertRecords);
        }
    }

    private void removePlayHistory(Collection<PlayHistoryRecord> current,
                                   Collection<PlayHistoryRecord> remote) {
        List<PlayHistoryRecord> removeRecords = new ArrayList<>(current);
        removeRecords.removeAll(remote);

        if (!removeRecords.isEmpty()) {
            recentlyPlayedStorage.removeRecentlyPlayed(removeRecords);
        }
    }

    // todo: we should relay on repositories to fetch missing contexts on demand!
    private void preloadNewContexts(List<PlayHistoryRecord> addRecords) throws Exception {
        SparseArray<Set<Urn>> buckets = contextBuckets(addRecords);

        for (int i = 0; i < buckets.size(); i++) {
            int contextType = buckets.keyAt(i);
            List<Urn> urns = new ArrayList<>(buckets.valueAt(i));

            switch (contextType) {
                case CONTEXT_PLAYLIST:
                    Collection<ApiPlaylist> playlists = fetchPlaylistsCommand.with(urns).call();
                    storePlaylistsCommand.call(playlists);
                    break;
                case CONTEXT_ARTIST:
                    Collection<ApiUser> users = fetchUsersCommand.with(urns).call();
                    storeUsersCommand.call(users);
                    break;
                case CONTEXT_TRACK_STATION:
                case CONTEXT_ARTIST_STATION:
                    fetchAndStoreStationsCommand.call(urns);
                    break;
            }
        }
    }

    private SparseArray<Set<Urn>> contextBuckets(List<PlayHistoryRecord> addRecords) {
        SparseArray<Set<Urn>> buckets = new SparseArray<>(4);

        for (PlayHistoryRecord addRecord : addRecords) {
            int contextType = addRecord.getContextType();

            if (buckets.indexOfKey(contextType) < 0) {
                Set<Urn> urns = new HashSet<>(1);
                urns.add(addRecord.contextUrn());
                buckets.put(contextType, urns);
            } else {
                buckets.get(contextType).add(addRecord.contextUrn());
            }
        }
        return buckets;
    }

}
