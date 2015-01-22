package com.soundcloud.android.offline;

import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.LoadDownloadsPendingRemovalCommand;
import com.soundcloud.android.offline.commands.StoreTrackDownloadsCommand;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfflineContentOperations {

    private final static long REMOVAL_DELAY = TimeUnit.MINUTES.toMillis(3);

    private final LikeOperations likeOperations;
    private final StoreTrackDownloadsCommand storeTrackDownloads;
    private final LoadDownloadsPendingRemovalCommand pendingRemoval;
    private final OfflineSettingsStorage settingsStorage;
    private final Scheduler scheduler;

    @Inject
    public OfflineContentOperations(StoreTrackDownloadsCommand storeTrackDownloads,
                                    LoadDownloadsPendingRemovalCommand pendingRemoval,
                                    LikeOperations operations,
                                    OfflineSettingsStorage settingsStorage,
                                    @Named("Storage") Scheduler scheduler) {
        this.storeTrackDownloads = storeTrackDownloads;
        this.pendingRemoval = pendingRemoval;
        this.likeOperations = operations;
        this.settingsStorage = settingsStorage;
        this.scheduler = scheduler;
    }

    public Observable<WriteResult> updateOfflineLikes() {
        return likeOperations
                .likedTrackUrns()
                .flatMap(storeTrackDownloads)
                .subscribeOn(scheduler);
    }

    public Observable<List<Urn>> pendingRemovals() {
        return pendingRemoval
                .with(REMOVAL_DELAY)
                .toObservable()
                .subscribeOn(scheduler);
    }

    public void setLikesOfflineSync(boolean isEnabled) {
        settingsStorage.setLikesOfflineSync(isEnabled);
    }

    public boolean isLikesOfflineSyncEnabled() {
        return settingsStorage.isLikesOfflineSyncEnabled();
    }

    public Observable<Boolean> getSettingsStatus() {
        return settingsStorage.getLikesOfflineSyncChanged();
    }
}
