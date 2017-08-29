package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.OldPlayQueueStorage;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueStorage;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class PlayQueueMigration implements Migration {

    private final OldPlayQueueStorage oldPlayQueueStorage;
    private final PlayQueueStorage playQueueStorage;
    private final Scheduler scheduler;

    @Inject
    public PlayQueueMigration(OldPlayQueueStorage oldPlayQueueStorage, PlayQueueStorage playQueueStorage, @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.oldPlayQueueStorage = oldPlayQueueStorage;
        this.playQueueStorage = playQueueStorage;
        this.scheduler = scheduler;
    }

    @Override
    public void applyMigration() {
        oldPlayQueueStorage.load()
                           .subscribeOn(scheduler)
                           .subscribe(new DefaultSingleObserver<List<PlayQueueItem>>() {
                               @Override
                               public void onSuccess(@NonNull List<PlayQueueItem> playQueueItems) {
                                   super.onSuccess(playQueueItems);
                                   playQueueStorage.store(PlayQueue.fromPlayQueueItems(playQueueItems));
                               }
                           });
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 751;
    }
}
