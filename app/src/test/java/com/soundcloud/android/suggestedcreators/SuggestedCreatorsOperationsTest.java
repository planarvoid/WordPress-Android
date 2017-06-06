package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.suggestedcreators.SuggestedCreatorsFixtures.createSuggestedCreators;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.configuration.experiments.SuggestedCreatorsExperiment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.stream.StreamItem.Kind;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class SuggestedCreatorsOperationsTest {

    private final long NOW = TimeUnit.DAYS.toMillis(1);
    private final long SIX_MINUTES_AGO = NOW - TimeUnit.MINUTES.toMillis(6);
    private final long FOUR_MINUTES_AGO = NOW - TimeUnit.MINUTES.toMillis(4);
    @Mock private FeatureFlags featureFlags;
    @Mock private MyProfileOperations myProfileOperations;
    @Mock private NewSyncOperations syncOperations;
    @Mock private SuggestedCreatorsStorage suggestedCreatorsStorage;
    @Mock private FollowingOperations followingOperations;
    @Mock private SuggestedCreatorsExperiment suggestedCreatorsExperiment;
    private Scheduler scheduler = Schedulers.trampoline();
    private CurrentDateProvider dateProvider;

    private SuggestedCreatorsOperations operations;

    @Before
    public void setup() {
        dateProvider = new TestDateProvider(NOW);
        operations = new SuggestedCreatorsOperations(featureFlags,
                                                     myProfileOperations,
                                                     syncOperations,
                                                     suggestedCreatorsStorage,
                                                     scheduler,
                                                     followingOperations,
                                                     dateProvider,
                                                     suggestedCreatorsExperiment);
        when(featureFlags.isEnabled(Flag.SUGGESTED_CREATORS)).thenReturn(true);
        when(featureFlags.isEnabled(Flag.FORCE_SUGGESTED_CREATORS_FOR_ALL)).thenReturn(false);
        when(syncOperations.lazySyncIfStale(Syncable.SUGGESTED_CREATORS)).thenReturn(Single.just(SyncResult.synced()));
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Single.just(Collections.emptyList()));
    }

    @Test
    public void returnsNotificationItemIfNumberOfFollowingsLowerEqualThanFive() {
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(3,
                                                                                 SuggestedCreatorRelation.LIKED);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Single.just(
                suggestedCreators));

        when(myProfileOperations.followingsUserAssociations()).thenReturn(Single.just(
                generateNonUserFollowings(5)));

        final TestObserver<StreamItem> subscriber = operations.suggestedCreators().test().assertValueCount(1);
        final StreamItem notificationItem = subscriber.values().get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void returnsNotificationItemIfNumberOfFollowingsIsGreaterThanLimitAndForceFeatureFlag() {
        when(featureFlags.isEnabled(Flag.FORCE_SUGGESTED_CREATORS_FOR_ALL)).thenReturn(true);
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(3, SuggestedCreatorRelation.LIKED);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Single.just(suggestedCreators));
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Single.just(generateNonUserFollowings(6)));

        final TestObserver<StreamItem> subscriber = operations.suggestedCreators().test().assertValueCount(1);
        final StreamItem notificationItem = subscriber.values().get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void filtersOutCreatorsAlreadyFollowed() {
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(2, SuggestedCreatorRelation.LIKED);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Single.just(
                suggestedCreators));

        final List<UserAssociation> usedUrns = Lists.newArrayList(createUserAssociation(suggestedCreators.get(0)
                                                                                                         .getCreator()
                                                                                                         .urn()));
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Single.just(usedUrns));

        final TestObserver<StreamItem> subscriber = operations.suggestedCreators().test().assertValueCount(1);
        final StreamItem.SuggestedCreators notificationItem = (StreamItem.SuggestedCreators) subscriber
                .values()
                .get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
        assertThat(notificationItem.suggestedCreators().size()).isEqualTo(1);
        assertThat(notificationItem.suggestedCreators()
                                   .get(0)).isEqualTo(SuggestedCreatorItem.fromSuggestedCreator(
                suggestedCreators.get(1)));
    }

    @Test
    public void filtersOutCreatorsAlreadyFollowedInThePast() {
        final Date timeInPast = new Date(SIX_MINUTES_AGO);
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(2,
                                                                                 SuggestedCreatorRelation.LIKED,
                                                                                 timeInPast);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Single.just(
                suggestedCreators));
        final List<UserAssociation> usedUrns = Lists.newArrayList(createUserAssociation(suggestedCreators.get(0)
                                                                                                         .getCreator()
                                                                                                         .urn(), timeInPast));
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Single.just(usedUrns));

        final TestObserver<StreamItem> subscriber = operations.suggestedCreators().test().assertValueCount(1);
        final StreamItem.SuggestedCreators notificationItem = (StreamItem.SuggestedCreators) subscriber
                .values()
                .get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
        assertThat(notificationItem.suggestedCreators().size()).isEqualTo(1);
        assertThat(notificationItem.suggestedCreators()
                                   .get(0)).isEqualTo(SuggestedCreatorItem.fromSuggestedCreator(
                suggestedCreators.get(1)));
    }

    @Test
    public void doesNotFilterOutCreatorsAlreadyFollowedRecently() {
        final Date recentTime = new Date(FOUR_MINUTES_AGO);
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(2,
                                                                                 SuggestedCreatorRelation.LIKED,
                                                                                 recentTime);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Single.just(
                suggestedCreators));

        final List<UserAssociation> usedUrns = Lists.newArrayList(createUserAssociation(suggestedCreators.get(0)
                                                                                                         .getCreator()
                                                                                                         .urn(), recentTime));
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Single.just(usedUrns));

        final TestObserver<StreamItem> subscriber = operations.suggestedCreators().test().assertValueCount(1);
        final StreamItem.SuggestedCreators notificationItem = (StreamItem.SuggestedCreators) subscriber
                .values()
                .get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
        assertThat(notificationItem.suggestedCreators().size()).isEqualTo(2);
        assertThat(notificationItem.suggestedCreators().get(0)).isEqualTo(SuggestedCreatorItem.fromSuggestedCreator(
                suggestedCreators.get(0)));
        assertThat(notificationItem.suggestedCreators().get(1)).isEqualTo(SuggestedCreatorItem.fromSuggestedCreator(
                suggestedCreators.get(1)));
    }

    @Test
    public void doesNotEmitItemWhenAllSuggestedCreatorsFilteredOut() {
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(2,
                                                                                 SuggestedCreatorRelation.LIKED);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Single.just(
                suggestedCreators));

        final List<UserAssociation> usedUrns = Lists.newArrayList(createUserAssociation(suggestedCreators.get(0)
                                                                                                         .getCreator()
                                                                                                         .urn()),
                                                                  createUserAssociation(suggestedCreators.get(1)
                                                                                                         .getCreator()
                                                                                                         .urn()));
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Single.just(usedUrns));

        operations.suggestedCreators().test().assertNoValues();
    }

    @Test
    public void returnsEmptyIfNumberOfFollowingsGreaterThanFive() {
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Single.just(
                generateNonUserFollowings(6)));

        operations.suggestedCreators().test().assertNoValues();
    }

    @Test
    public void toggleFollowCallsStorageAndFollowingsOperation() {
        final Urn urn = Urn.forUser(1);
        final boolean isFollowing = true;
        when(suggestedCreatorsStorage.toggleFollowSuggestedCreator(urn, isFollowing)).thenReturn(Completable.complete());
        when(followingOperations.toggleFollowing(urn, isFollowing)).thenReturn(Completable.complete());

        operations.toggleFollow(urn, isFollowing).subscribe();

        verify(followingOperations).toggleFollowing(urn, isFollowing);
        verify(suggestedCreatorsStorage).toggleFollowSuggestedCreator(urn, isFollowing);
    }

    private List<UserAssociation> generateNonUserFollowings(int numberOfUrns) {
        final List<UserAssociation> userAssociations = Lists.newArrayList();
        for (int i = 0; i < numberOfUrns; i++) {
            userAssociations.add(createUserAssociation(new Urn("soundcloud:follower:" + i)));
        }
        return userAssociations;
    }

    @NonNull
    private UserAssociation createUserAssociation(Urn urn) {
        return UserAssociation.create(urn,
                                      0,
                                      0,
                                      Optional.absent(),
                                      Optional.absent());
    }

    @NonNull
    private UserAssociation createUserAssociation(Urn urn, Date addedAt) {
        return UserAssociation.create(urn,
                                      0,
                                      0,
                                      Optional.of(addedAt),
                                      Optional.absent());
    }
}
