package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.configuration.experiments.SuggestedCreatorsExperiment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SuggestedCreatorsOperations {
    private static final long VISIBLE_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    private static final int FOLLOWINGS_LIMIT = 5;
    private final Predicate<List<UserAssociation>> lessThanLimitFollowers = new Predicate<List<UserAssociation>>() {
        @Override
        public boolean test(List<UserAssociation> userAssociations) {
            return userAssociations.size() <= FOLLOWINGS_LIMIT || featureFlags.isEnabled(Flag.FORCE_SUGGESTED_CREATORS_FOR_ALL);
        }
    };
    private final FeatureFlags featureFlags;
    private final MyProfileOperations myProfileOperations;
    private final NewSyncOperations syncOperations;
    private final SuggestedCreatorsStorage suggestedCreatorsStorage;
    private final Scheduler scheduler;
    private final FollowingOperations followingOperations;
    private final DateProvider dateProvider;
    private final SuggestedCreatorsExperiment suggestedCreatorsExperiment;

    @Inject
    SuggestedCreatorsOperations(FeatureFlags featureFlags,
                                MyProfileOperations myProfileOperations,
                                NewSyncOperations syncOperations,
                                SuggestedCreatorsStorage suggestedCreatorsStorage,
                                @Named(RX_HIGH_PRIORITY) Scheduler scheduler,
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

    public Maybe<StreamItem> suggestedCreators() {
        if (featureFlags.isEnabled(Flag.SUGGESTED_CREATORS) || suggestedCreatorsExperiment.isEnabled()) {
            return myProfileOperations.followingsUserAssociations()
                                      .filter(lessThanLimitFollowers)
                                      .flatMap(this::loadSuggestedCreators);
        }
        return Maybe.empty();
    }

    Completable toggleFollow(Urn urn, boolean isFollowing) {
        return Completable.mergeArray(followingOperations.toggleFollowing(urn, isFollowing),
                                      suggestedCreatorsStorage.toggleFollowSuggestedCreator(urn, isFollowing).subscribeOn(scheduler));
    }

    private Maybe<StreamItem> loadSuggestedCreators(List<UserAssociation> userAssociations) {
        return lazySyncCreators().map(filterOutAlreadyFollowed(userAssociations))
                                 .filter(list -> !list.isEmpty())
                                 .map(StreamItem::forSuggestedCreators);
    }

    private Function<List<SuggestedCreator>, List<SuggestedCreator>> filterOutAlreadyFollowed(final List<UserAssociation> userAssociations) {
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

    private Single<List<SuggestedCreator>> lazySyncCreators() {
        return load(syncOperations.lazySyncIfStale(Syncable.SUGGESTED_CREATORS));
    }

    private Single<List<SuggestedCreator>> load(Single<SyncResult> source) {
        return source.flatMap(o -> suggestedCreatorsStorage.suggestedCreators()
                                                           .subscribeOn(scheduler));
    }
}
