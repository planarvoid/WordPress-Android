package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.IS_NOT_EMPTY_LIST;
import static com.soundcloud.android.rx.RxUtils.ZIP_TO_VOID;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.configuration.experiments.SuggestedCreatorsExperiment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SuggestedCreatorsOperations {
    private static final long VISIBLE_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    private static final int FOLLOWINGS_LIMIT = 5;
    private final Func1<List<UserAssociation>, Boolean> lessThanLimitFollowers = new Func1<List<UserAssociation>, Boolean>() {
        @Override
        public Boolean call(List<UserAssociation> userAssociations) {
            return userAssociations.size() <= FOLLOWINGS_LIMIT || featureFlags.isEnabled(Flag.FORCE_SUGGESTED_CREATORS_FOR_ALL);
        }
    };
    private final FeatureFlags featureFlags;
    private final MyProfileOperations myProfileOperations;
    private final SyncOperations syncOperations;
    private final SuggestedCreatorsStorage suggestedCreatorsStorage;
    private final Scheduler scheduler;
    private final FollowingOperations followingOperations;
    private final DateProvider dateProvider;
    private final SuggestedCreatorsExperiment suggestedCreatorsExperiment;

    @Inject
    SuggestedCreatorsOperations(FeatureFlags featureFlags,
                                MyProfileOperations myProfileOperations,
                                SyncOperations syncOperations,
                                SuggestedCreatorsStorage suggestedCreatorsStorage,
                                @Named(HIGH_PRIORITY) Scheduler scheduler,
                                FollowingOperations followingOperations,
                                CurrentDateProvider dateProvider,
                                SuggestedCreatorsExperiment suggestedCreatorsExperiment) {
        this.featureFlags = featureFlags;
        this.myProfileOperations = myProfileOperations;
        this.syncOperations = syncOperations;
        this.suggestedCreatorsStorage = suggestedCreatorsStorage;
        this.scheduler = scheduler;
        this.followingOperations = followingOperations;
        this.dateProvider = dateProvider;
        this.suggestedCreatorsExperiment = suggestedCreatorsExperiment;
    }

    public Observable<StreamItem> suggestedCreators() {
        if (featureFlags.isEnabled(Flag.SUGGESTED_CREATORS) || suggestedCreatorsExperiment.isEnabled()) {
            return myProfileOperations.followingsUserAssociations()
                                      .filter(lessThanLimitFollowers)
                                      .flatMap(loadSuggestedCreators());
        }
        return Observable.empty();
    }

    Observable<Void> toggleFollow(Urn urn, boolean isFollowing) {
        return Observable.combineLatest(
                followingOperations.toggleFollowing(urn, isFollowing),
                suggestedCreatorsStorage.toggleFollowSuggestedCreator(urn, isFollowing).subscribeOn(scheduler),
                ZIP_TO_VOID
        );
    }

    private Func1<List<UserAssociation>, Observable<StreamItem>> loadSuggestedCreators() {
        return userAssociations -> lazySyncCreators().map(filterOutAlreadyFollowed(userAssociations))
                                             .filter(IS_NOT_EMPTY_LIST)
                                             .map(StreamItem::forSuggestedCreators);
    }

    private Func1<List<SuggestedCreator>, List<SuggestedCreator>> filterOutAlreadyFollowed(final List<UserAssociation> userAssociations) {
        return suggestedCreators -> {
            final List<SuggestedCreator> result = Lists.newArrayList();
            final long currentTimeMillis = dateProvider.getCurrentTime();
            for (SuggestedCreator suggestedCreator : suggestedCreators) {
                boolean add = true;
                for (final UserAssociation userAssociation : userAssociations) {
                    final Optional<Date> followedAt = suggestedCreator.followedAt();
                    final boolean wasRecentlyFollowed = followedAt.isPresent() && followedAt.get().getTime() > (currentTimeMillis - VISIBLE_INTERVAL);
                    if (userAssociation.userUrn().equals(suggestedCreator.getCreator().urn()) && !wasRecentlyFollowed) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    result.add(suggestedCreator);
                }
            }
            return result;
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
