package com.soundcloud.android.likes;


import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class LikeOperations {

    private static final String TAG = "LikeOperations";

    private final LoadLikedTracksCommand loadLikedTracksCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;

    @Inject
    public LikeOperations(LoadLikedTracksCommand loadLikedTracksCommand,
                          @Named("Storage") Scheduler scheduler, SyncInitiator syncInitiator) {
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
    }

    public Observable<List<PropertySet>> likedTracks() {
        return loadLikedTracksCommand.toObservable().subscribeOn(scheduler)
                .flatMap(new Func1<List<PropertySet>, Observable<List<PropertySet>>>() {
                    @Override
                    public Observable<List<PropertySet>> call(List<PropertySet> result) {
                        if (result.isEmpty()) {
                            return updatedLikedTracks();
                        } else {
                            return Observable.just(result);
                        }
                    }
                });
    }

    public Observable<List<PropertySet>> updatedLikedTracks() {
        return syncInitiator.syncTrackLikes().flatMap(handleSyncResult());
    }

    private Func1<SyncResult, Observable<List<PropertySet>>> handleSyncResult() {
        return new Func1<SyncResult, Observable<List<PropertySet>>>() {
            @Override
            public Observable<List<PropertySet>> call(SyncResult syncResultEvent) {
                Log.d(TAG, "Sync finished; result = " + syncResultEvent);
                return loadLikedTracksCommand.toObservable();
            }
        };
    }
}
