package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.IS_NOT_EMPTY_LIST;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stream.SoundStreamItem;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.collections.Lists;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class SuggestedCreatorsOperations {
    private static final int FOLLOWINGS_LIMIT = 5;
    private static final Func1<List<SuggestedCreator>, SoundStreamItem> TO_SOUND_STREAM_ITEM = new Func1<List<SuggestedCreator>, SoundStreamItem>() {
        @Override
        public SoundStreamItem call(List<SuggestedCreator> suggestedCreators) {
            return SoundStreamItem.forSuggestedCreators(suggestedCreators);
        }
    };
    private final Func1<List<Urn>, Boolean> lessThanLimitFollowers = new Func1<List<Urn>, Boolean>() {
        @Override
        public Boolean call(List<Urn> urns) {
            return urns.size() <= FOLLOWINGS_LIMIT || featureFlags.isEnabled(Flag.FORCE_SUGGESTED_CREATORS_FOR_ALL);
        }
    };
    private final FeatureFlags featureFlags;
    private final MyProfileOperations myProfileOperations;
    private final SyncOperations syncOperations;
    private final SuggestedCreatorsStorage suggestedCreatorsStorage;
    private final Scheduler scheduler;

    @Inject
    SuggestedCreatorsOperations(FeatureFlags featureFlags,
                                MyProfileOperations myProfileOperations,
                                SyncOperations syncOperations,
                                SuggestedCreatorsStorage suggestedCreatorsStorage,
                                @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.featureFlags = featureFlags;
        this.myProfileOperations = myProfileOperations;
        this.syncOperations = syncOperations;
        this.suggestedCreatorsStorage = suggestedCreatorsStorage;
        this.scheduler = scheduler;
    }

    public Observable<SoundStreamItem> suggestedCreators() {
        if (featureFlags.isEnabled(Flag.SUGGESTED_CREATORS)) {
            return myProfileOperations.followingsUrns()
                                      .filter(lessThanLimitFollowers)
                                      .flatMap(loadSuggestedCreators());
        }
        return Observable.empty();
    }

    private Func1<List<Urn>, Observable<SoundStreamItem>> loadSuggestedCreators() {
        return new Func1<List<Urn>, Observable<SoundStreamItem>>() {
            public Observable<SoundStreamItem> call(final List<Urn> urns) {
                return lazySyncCreators().map(filterOutAlreadyFollowed(urns))
                                         .filter(IS_NOT_EMPTY_LIST)
                                         .map(TO_SOUND_STREAM_ITEM);
            }
        };
    }

    private Func1<List<SuggestedCreator>, List<SuggestedCreator>> filterOutAlreadyFollowed(final List<Urn> urns) {
        return new Func1<List<SuggestedCreator>, List<SuggestedCreator>>() {
            public List<SuggestedCreator> call(List<SuggestedCreator> suggestedCreators) {
                final List<SuggestedCreator> result = Lists.newArrayList();
                for (SuggestedCreator suggestedCreator : suggestedCreators) {
                    if (!urns.contains(suggestedCreator.getCreator().urn())) {
                        result.add(suggestedCreator);
                    }
                }
                return result;
            }
        };
    }

    private Observable<List<SuggestedCreator>> lazySyncCreators() {
        return load(syncOperations.lazySyncIfStale(Syncable.SUGGESTED_CREATORS));
    }

    private Observable<List<SuggestedCreator>> load(Observable<SyncOperations.Result> source) {
        return source.flatMap(continueWith(suggestedCreatorsStorage.suggestedCreators()
                                                                   .subscribeOn(scheduler)));
    }
}
