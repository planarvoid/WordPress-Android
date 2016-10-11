package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.suggestedcreators.SuggestedCreatorsFixtures.createSuggestedCreators;
import static com.soundcloud.android.sync.SyncOperations.Result.SYNCED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stream.SoundStreamItem;
import com.soundcloud.android.stream.SoundStreamItem.Kind;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.collections.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class SuggestedCreatorsOperationsTest {
    @Mock private FeatureFlags featureFlags;
    @Mock private MyProfileOperations myProfileOperations;
    @Mock private SyncOperations syncOperations;
    @Mock private SuggestedCreatorsStorage suggestedCreatorsStorage;
    private Scheduler scheduler = Schedulers.immediate();

    private SuggestedCreatorsOperations operations;
    private TestSubscriber<SoundStreamItem> subscriber;

    @Before
    public void setup() {
        operations = new SuggestedCreatorsOperations(featureFlags,
                                                     myProfileOperations,
                                                     syncOperations,
                                                     suggestedCreatorsStorage,
                                                     scheduler);
        when(featureFlags.isEnabled(Flag.SUGGESTED_CREATORS)).thenReturn(true);
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

        when(myProfileOperations.followingsUrns()).thenReturn(Observable.just(
                generateNonUserFollowingUrns(5)));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SoundStreamItem notificationItem = subscriber.getOnNextEvents().get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void filtersOutCreatorsAlreadyFollowed() {
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(2,
                                                                                 SuggestedCreatorRelation.LIKED);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Observable.just(
                suggestedCreators));

        final List<Urn> usedUrns = Lists.newArrayList(suggestedCreators.get(0).getCreator().urn());
        when(myProfileOperations.followingsUrns()).thenReturn(Observable.just(usedUrns));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SoundStreamItem.SuggestedCreators notificationItem = (SoundStreamItem.SuggestedCreators) subscriber
                .getOnNextEvents()
                .get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
        assertThat(notificationItem.suggestedCreators().size()).isEqualTo(1);
        assertThat(notificationItem.suggestedCreators()
                                   .get(0)).isEqualTo(SuggestedCreatorItem.fromSuggestedCreator(
                suggestedCreators.get(1)));
    }

    @Test
    public void doesNotEmitItemWhenAllSuggestedCreatorsFilteredOut() {
        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(2,
                                                                                 SuggestedCreatorRelation.LIKED);
        when(suggestedCreatorsStorage.suggestedCreators()).thenReturn(Observable.just(
                suggestedCreators));

        final List<Urn> usedUrns = Lists.newArrayList(suggestedCreators.get(0).getCreator().urn(),
                                                      suggestedCreators.get(1).getCreator().urn());
        when(myProfileOperations.followingsUrns()).thenReturn(Observable.just(usedUrns));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertNoValues();
    }

    @Test
    public void returnsEmptyIfNumberOfFollowingsGreaterThanFive() {
        when(myProfileOperations.followingsUrns()).thenReturn(Observable.just(
                generateNonUserFollowingUrns(6)));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertNoValues();
    }

    private List<Urn> generateNonUserFollowingUrns(int numberOfUrns) {
        final List<Urn> urns = Lists.newArrayList();
        for (int i = 0; i < numberOfUrns; i++) {
            urns.add(new Urn("soundcloud:follower:" + i));
        }
        return urns;
    }
}
