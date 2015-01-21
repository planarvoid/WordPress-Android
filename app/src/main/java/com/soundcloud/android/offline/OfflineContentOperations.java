package com.soundcloud.android.offline;

import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.offline.commands.StoreTrackDownloadsCommand;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

public class OfflineContentOperations {

    private final LikeOperations likeOperations;
    private final StoreTrackDownloadsCommand storeTrackDownloads;
    private final OfflineSettingsStorage settingsStorage;
    private final Scheduler scheduler;

    @Inject
    public OfflineContentOperations(StoreTrackDownloadsCommand storeTrackDownloads,
                                    LikeOperations operations,
                                    OfflineSettingsStorage settingsStorage,
                                    @Named("Storage") Scheduler scheduler) {
        this.storeTrackDownloads = storeTrackDownloads;
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
