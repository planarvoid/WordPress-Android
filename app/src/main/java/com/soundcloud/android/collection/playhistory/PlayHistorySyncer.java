package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

class PlayHistorySyncer implements Callable<Boolean> {

    private final PlayHistoryStorage playHistoryStorage;
    private final PushPlayHistoryCommand pushPlayHistoryCommand;
    private final FetchPlayHistoryCommand fetchPlayHistoryCommand;
    private final FetchTracksCommand fetchTracksCommand;
    private final StoreTracksCommand storeTracksCommand;
    private final EventBus eventBus;

    @Inject
    PlayHistorySyncer(PlayHistoryStorage playHistoryStorage,
                      FetchPlayHistoryCommand fetchPlayHistoryCommand,
                      PushPlayHistoryCommand pushPlayHistoryCommand,
                      FetchTracksCommand fetchTracksCommand,
                      StoreTracksCommand storeTracksCommand,
                      EventBus eventBus) {
        this.playHistoryStorage = playHistoryStorage;
        this.pushPlayHistoryCommand = pushPlayHistoryCommand;
        this.fetchPlayHistoryCommand = fetchPlayHistoryCommand;
        this.fetchTracksCommand = fetchTracksCommand;
        this.storeTracksCommand = storeTracksCommand;
        this.eventBus = eventBus;
    }

    @Override
    public Boolean call() throws Exception {
        boolean hasChanges = updateLocalStorage();
        pushUnSyncedPlayHistory();
        return hasChanges;
    }

    private void pushUnSyncedPlayHistory() {
        pushPlayHistoryCommand.call();
    }

    private boolean updateLocalStorage() throws Exception {
        Collection<PlayHistoryRecord> remote = fetchPlayHistoryCommand.call();
        Collection<PlayHistoryRecord> current = playHistoryStorage.loadSyncedPlayHistory();

        boolean hasChanges = !current.equals(remote);

        if (hasChanges) {
            addPlayHistory(current, remote);
            removePlayHistory(current, remote);
            eventBus.publish(EventQueue.PLAY_HISTORY, PlayHistoryEvent.fromAdded(Urn.NOT_SET));
        }

        return hasChanges;
    }

    private void addPlayHistory(Collection<PlayHistoryRecord> current,
                                Collection<PlayHistoryRecord> remote) throws Exception {
        List<PlayHistoryRecord> insertRecords = new ArrayList<>(remote);
        insertRecords.removeAll(current);

        if (!insertRecords.isEmpty()) {
            preloadNewTracks(insertRecords);
            playHistoryStorage.insertPlayHistory(insertRecords);
        }
    }

    private void removePlayHistory(Collection<PlayHistoryRecord> current,
                                   Collection<PlayHistoryRecord> remote) {
        List<PlayHistoryRecord> removeRecords = new ArrayList<>(current);
        removeRecords.removeAll(remote);

        if (!removeRecords.isEmpty()) {
            playHistoryStorage.removePlayHistory(removeRecords);
        }
    }

    private void preloadNewTracks(List<PlayHistoryRecord> addRecords) throws Exception {
        List<Urn> urns = new ArrayList<>(new HashSet<>(transform(addRecords, PlayHistoryRecord.TO_TRACK_URN)));
        Collection<ApiTrack> tracks = fetchTracksCommand.with(urns).call();
        storeTracksCommand.call(tracks);
    }

}
