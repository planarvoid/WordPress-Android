package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.suggestedcreators.SuggestedCreatorsFixtures.createSuggestedCreators;
import static com.soundcloud.android.sync.SyncOperations.Result.SYNCED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.stream.StreamItem.Kind;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

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
    @Mock private SyncOperations syncOperations;
    @Mock private SuggestedCreatorsStorage suggestedCreatorsStorage;
    @Mock private FollowingOperations followingOperations;
    private Scheduler scheduler = Schedulers.immediate();
    private CurrentDateProvider dateProvider;

    private SuggestedCreatorsOperations operations;
    private TestSubscriber<StreamItem> subscriber;

    @Before
    public void setup() {
        dateProvider = new TestDateProvider(NOW);
        operations = new SuggestedCreatorsOperations(featureFlags,
                                                     myProfileOperations,
                                                     syncOperations,
                                                     suggestedCreatorsStorage,
                                                     scheduler,
                                                     followingOperations,
                                                     dateProvider);
        when(featureFlags.isEnabled(Flag.SUGGESTED_CREATORS)).thenReturn(true);
        when(featureFlags.isEnabled(Flag.FORCE_SUGGESTED_CREATORS_FOR_ALL)).thenReturn(false);
        when(syncOperations.lazySyncIfStale(Syncable.SUGGESTED_CREATORS)).thenReturn(Observable.just(
                SYNCED));
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Observable.<List<SuggestedCreator>>empty());
        subscriber = new TestSubscriber<>();
    }

    @Test
    public void returnsNotificationItemIfNumberOfFollowingsLowerEqualThanFive() {
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(3,
                                                                                 SuggestedCreatorRelation.LIKED);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Observable.just(
                suggestedCreators));

        when(myProfileOperations.followingsUserAssociations()).thenReturn(Observable.just(
                generateNonUserFollowings(5)));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final StreamItem notificationItem = subscriber.getOnNextEvents().get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void returnsNotificationItemIfNumberOfFollowingsIsGreaterThanLimitAndForceFeatureFlag() {
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(6,
                                                                                 SuggestedCreatorRelation.LIKED);
        when(featureFlags.isEnabled(Flag.FORCE_SUGGESTED_CREATORS_FOR_ALL)).thenReturn(true);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Observable.just(
                suggestedCreators));

        when(myProfileOperations.followingsUserAssociations()).thenReturn(Observable.just(
                generateNonUserFollowings(5)));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final StreamItem notificationItem = subscriber.getOnNextEvents().get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void filtersOutCreatorsAlreadyFollowed() {
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(2,
                                                                                 SuggestedCreatorRelation.LIKED);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Observable.just(
                suggestedCreators));

        final List<UserAssociation> usedUrns = Lists.newArrayList(createUserAssociation(suggestedCreators.get(0)
                                                                                                         .getCreator()
                                                                                                         .urn()));
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Observable.just(usedUrns));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final StreamItem.SuggestedCreators notificationItem = (StreamItem.SuggestedCreators) subscriber
                .getOnNextEvents()
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
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Observable.just(
                suggestedCreators));
        final List<UserAssociation> usedUrns = Lists.newArrayList(createUserAssociation(suggestedCreators.get(0)
                                                                                                         .getCreator()
                                                                                                         .urn(), timeInPast));
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Observable.just(usedUrns));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final StreamItem.SuggestedCreators notificationItem = (StreamItem.SuggestedCreators) subscriber
                .getOnNextEvents()
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
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Observable.just(
                suggestedCreators));

        final List<UserAssociation> usedUrns = Lists.newArrayList(createUserAssociation(suggestedCreators.get(0)
                                                                                                         .getCreator()
                                                                                                         .urn(), recentTime));
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Observable.just(usedUrns));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final StreamItem.SuggestedCreators notificationItem = (StreamItem.SuggestedCreators) subscriber
                .getOnNextEvents()
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
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Observable.just(
                suggestedCreators));

        final List<UserAssociation> usedUrns = Lists.newArrayList(createUserAssociation(suggestedCreators.get(0)
                                                                                                         .getCreator()
                                                                                                         .urn()),
                                                                  createUserAssociation(suggestedCreators.get(1)
                                                                                                         .getCreator()
                                                                                                         .urn()));
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Observable.just(usedUrns));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertNoValues();
    }

    @Test
    public void returnsEmptyIfNumberOfFollowingsGreaterThanFive() {
        when(myProfileOperations.followingsUserAssociations()).thenReturn(Observable.just(
                generateNonUserFollowings(6)));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertNoValues();
    }

    @Test
    public void toggleFollowCallsStorageAndFollowingsOperation() {
        final Urn urn = Urn.forUser(1);
        final boolean isFollowing = true;
        when(suggestedCreatorsStorage.toggleFollowSuggestedCreator(urn,
                                                                   isFollowing)).thenReturn(Observable.<ChangeResult>empty());
        when(followingOperations.toggleFollowing(urn, isFollowing)).thenReturn(Observable.<PropertySet>empty());

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
                                      Optional.<Date>absent(),
                                      Optional.<Date>absent());
    }

    @NonNull
    private UserAssociation createUserAssociation(Urn urn, Date addedAt) {
        return UserAssociation.create(urn,
                                      0,
                                      0,
                                      Optional.of(addedAt),
                                      Optional.<Date>absent());
    }
}
