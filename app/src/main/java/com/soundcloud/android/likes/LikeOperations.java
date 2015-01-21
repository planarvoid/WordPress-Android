package com.soundcloud.android.likes;


import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadLikedPlaylistsCommand;
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
    private final LoadLikedPlaylistsCommand loadLikedPlaylistsCommand;
    private final LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;

    @Inject
    public LikeOperations(LoadLikedTracksCommand loadLikedTracksCommand,
                          LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand,
                          LoadLikedPlaylistsCommand loadLikedPlaylistsCommand,
                          SyncInitiator syncInitiator,
                          @Named("Storage") Scheduler scheduler) {
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.loadLikedPlaylistsCommand = loadLikedPlaylistsCommand;
        this.loadLikedTrackUrnsCommand = loadLikedTrackUrnsCommand;
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

    public Observable<List<PropertySet>> likedPlaylists() {
        return loadLikedPlaylistsCommand.toObservable().subscribeOn(scheduler);
    }

    public Observable<List<Urn>> likedTrackUrns() {
        return loadLikedTrackUrnsCommand.toObservable().subscribeOn(scheduler);
    }
}
