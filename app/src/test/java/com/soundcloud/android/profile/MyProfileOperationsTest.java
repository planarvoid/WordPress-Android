package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.MyProfileOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.Pager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyProfileOperationsTest extends AndroidUnitTest {

    private final Observable<Void> SHOULD_NEVER_SYNC = Observable.error(new RuntimeException("should not have synced"));
    private MyProfileOperations operations;

    @Mock private PostsStorage postStorage;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private SyncInitiator syncInitiator;
    @Mock private UserAssociationStorage userAssociationStorage;

    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<List<Following>> subscriber;

    @Before
    public void setUp() throws Exception {
        operations = new MyProfileOperations(
                postStorage,
                syncInitiatorBridge,
                syncInitiator,
                userAssociationStorage,
                scheduler);

        subscriber = new TestSubscriber<>();
    }

    @Test
    public void syncAndLoadFollowingsWhenInitialFollowingsLoadReturnsEmptyList() {
        final List<Following> firstPage = createPageOfFollowings(PAGE_SIZE);
        final List<Urn> followingsUrn = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(
                followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE,
                                                  Consts.NOT_SET)).thenReturn(Observable.just(Collections.emptyList()),
                                                                              Observable.just(firstPage));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Observable.just(null));
        when(syncInitiator.batchSyncUsers(followingsUrn)).thenReturn(Observable.just(SyncJobResult.success("success",
                                                                                                            true)));

        operations.pagedFollowings().subscribe(subscriber);

        subscriber.assertValue(firstPage);
    }

    @Test
    public void syncAndLoadEmptyFollowingsResultsWithEmptyResults() {
        when(userAssociationStorage.followedUserUrns(PAGE_SIZE,
                                                     Consts.NOT_SET)).thenReturn(Observable.just(Collections.emptyList()));
        when(userAssociationStorage.followedUsers(PAGE_SIZE,
                                                  Consts.NOT_SET)).thenReturn(Observable.just(Collections.emptyList()));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Observable.just(null));

        operations.pagedFollowings().subscribe(subscriber);

        subscriber.assertValue(Collections.emptyList());
    }

    @Test
    public void pagedFollowingsReturnsFollowingsFromStorage() {
        final List<Following> pageOfFollowings = createPageOfFollowings(2);
        final List<Urn> urns = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));
        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(urns));
        when(userAssociationStorage.followedUsers(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(
                pageOfFollowings));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(SHOULD_NEVER_SYNC);
        when(syncInitiator.batchSyncUsers(urns)).thenReturn(Observable.just(SyncJobResult.success("success", true)));

        operations.pagedFollowings().subscribe(subscriber);

        subscriber.assertValue(pageOfFollowings);
    }

    @Test
    public void followingsPagerLoadsNextPageUsingPositionOfLastItemOfPreviousPage() {
        final List<Following> firstPage = createPageOfFollowings(PAGE_SIZE);
        final List<Following> secondPage = createPageOfFollowings(1);
        final List<Urn> followingsUrn = pageOfUrns(firstPage);
        final long position = firstPage.get(PAGE_SIZE - 1).userAssociation().position();

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, position)).thenReturn(Observable.just(followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE, position)).thenReturn(Observable.just(secondPage));

        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Observable.empty());
        when(syncInitiator.batchSyncUsers(followingsUrn)).thenReturn(Observable.just(SyncJobResult.success("success",
                                                                                                      true)));

        operations.followingsPagingFunction().call(firstPage).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(secondPage));
    }

    @Test
    public void followingsPagerFinishesIfLastPageIncomplete() {
        assertThat(operations.followingsPagingFunction()
                             .call(createPageOfFollowings(PAGE_SIZE - 1))).isEqualTo(Pager.finish());
    }

    @Test
    public void updatedFollowingsReloadsFollowingsAfterSyncWithChange() {
        final List<Following> pageOfFollowings1 = createPageOfFollowings(2);
        final List<Urn> followingsUrn = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(
                followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(
                pageOfFollowings1));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Observable.just(null));
        when(syncInitiator.batchSyncUsers(followingsUrn)).thenReturn(Observable.just(SyncJobResult.success("success",
                                                                                                           true)));
        final List<Following> pageOfFollowings = pageOfFollowings1;

        operations.updatedFollowings().subscribe(subscriber);

        subscriber.assertValue(pageOfFollowings);
    }

    @Test
    public void shouldLoadLastPublicPostedTrack() {
        LastPostedTrack trackOpt = PlayableFixtures.expectedLastPostedTrackForPostsScreen();
        when(postStorage.loadLastPublicPostedTrack()).thenReturn(Observable.just(trackOpt));
        TestSubscriber<LastPostedTrack> subscriber = new TestSubscriber<>();

        operations.lastPublicPostedTrack().subscribe(subscriber);

        subscriber.assertValue(trackOpt);
    }

    @Test
    public void returnsListOfFollowingsUrns() {
        TestSubscriber<List<UserAssociation>> subscriber = new TestSubscriber<>();
        final UserAssociation userAssociation1 = createUserAssociation(Urn.forUser(123L));
        final UserAssociation userAssociation2 = createUserAssociation(Urn.forUser(124L));
        final List<UserAssociation> followingsUrn = Arrays.asList(userAssociation1, userAssociation2);

        when(userAssociationStorage.followedUserAssociations()).thenReturn(Observable.just(
                followingsUrn));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(SHOULD_NEVER_SYNC);

        operations.followingsUserAssociations().subscribe(subscriber);

        subscriber.assertCompleted();
        subscriber.assertValue(followingsUrn);

        verify(syncInitiatorBridge, never()).refreshFollowings();
    }

    @Test
    public void syncsWhenStoredFollowingsListEmpty() {
    TestSubscriber<List<UserAssociation>> subscriber = new TestSubscriber<>();

        when(userAssociationStorage.followedUserAssociations()).thenReturn(Observable.just(Collections.emptyList()));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Observable.just(null));

        operations.followingsUserAssociations().subscribe(subscriber);

        subscriber.assertCompleted();
        subscriber.assertValue(Collections.emptyList());

        verify(syncInitiatorBridge).refreshFollowings();
        verify(userAssociationStorage, times(2)).followedUserAssociations();
    }

    private UserAssociation createUserAssociation(Urn urn) {
        return UserAssociation.create(urn, 0, 1, Optional.absent(), Optional.absent());
    }

    private List<Following> createPageOfFollowings(int size) {
        List<Following> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            page.add(PlayableFixtures.expectedFollowingForFollowingsScreen(i));
        }
        return page;
    }

    private List<Urn> pageOfUrns(List<Following> followings) {
        List<Urn> page = new ArrayList<>(followings.size());
        for (Following following : followings) {
            page.add(following.userAssociation().userUrn());
        }
        return page;
    }
}
