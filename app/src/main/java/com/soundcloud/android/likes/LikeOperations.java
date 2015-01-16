package com.soundcloud.android.likes;


import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class LikeOperations {

    private final LoadLikedTracksCommand loadLikedTracksCommand;
    private final Scheduler scheduler;

    @Inject
    public LikeOperations(LoadLikedTracksCommand loadLikedTracksCommand,
                          @Named("Storage") Scheduler scheduler) {
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.scheduler = scheduler;
    }

    public Observable<List<PropertySet>> likedTracks() {
        return loadLikedTracksCommand.toObservable().subscribeOn(scheduler);
    }

}
