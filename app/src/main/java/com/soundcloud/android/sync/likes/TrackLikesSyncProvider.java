package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TrackLikesSyncProvider extends SyncerRegistry.SyncProvider {

    private final Provider<LikesSyncer<ApiTrack>> trackLikesSyncer;
    private final MyTrackLikesStateProvider myTrackLikesStateProvider;

    @Inject
    public TrackLikesSyncProvider(@Named(LikesSyncModule.TRACK_LIKES_SYNCER) Provider<LikesSyncer<ApiTrack>> trackLikesSyncer,
                                  MyTrackLikesStateProvider myTrackLikesStateProvider) {
        super(Syncable.TRACK_LIKES);
        this.trackLikesSyncer = trackLikesSyncer;
        this.myTrackLikesStateProvider = myTrackLikesStateProvider;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return trackLikesSyncer.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return myTrackLikesStateProvider.hasLocalChanges();
    }

    @Override
    public long staleTime() {
        return TimeUnit.HOURS.toMillis(1);
    }

    @Override
    public boolean usePeriodicSync() {
        return true;
    }
}
