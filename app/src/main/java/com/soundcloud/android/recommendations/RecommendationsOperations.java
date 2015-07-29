package com.soundcloud.android.recommendations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class RecommendationsOperations {

    private final Func1<SyncResult, Observable<List<PropertySet>>> toSeedTracks = new Func1<SyncResult, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(SyncResult ignored) {
            return recommendationsStorage.seedTracks()
                    .subscribeOn(scheduler);
        }
    };

    private final SyncInitiator syncInitiator;
    private final RecommendationsStorage recommendationsStorage;
    private final Scheduler scheduler;

    @Inject
    RecommendationsOperations(SyncInitiator syncInitiator,
                              RecommendationsStorage recommendationsStorage,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.syncInitiator = syncInitiator;
        this.recommendationsStorage = recommendationsStorage;
        this.scheduler = scheduler;
    }

    public Observable<List<PropertySet>> recommendations() {
        return syncInitiator.syncRecommendations().flatMap(toSeedTracks);
    }

    public Observable<List<Urn>> recommendationsWithSeedTrack(long seedTrackLocalId, final Urn seedTrackUrn){
        return recommendationsForSeedTrack(seedTrackLocalId)
                .map(new Func1<List<Urn>, List<Urn>>() {
                    @Override
                    public List<Urn> call(List<Urn> urns) {
                        urns.add(0, seedTrackUrn);
                        return urns;
                    }
                });
    }

    public Observable<List<Urn>> recommendationsForSeedTrack(long seedTrackLocalId){
        return recommendationsStorage.recommendations(seedTrackLocalId)
                .subscribeOn(scheduler);
    }

}
