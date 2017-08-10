package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.MyProfileOperations.PAGE_SIZE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.Pager;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.observers.TestSubscriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MyProfileOperationsTest {

    private static final SyncJobResult SYNC_JOB_RESULT = SyncJobResult.success("success", true);
    private final Single<SyncJobResult> SHOULD_NEVER_SYNC = Single.error(new RuntimeException("should not have synced"));
    private MyProfileOperations operations;

    @Mock private PostsStorage postStorage;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private SyncInitiator syncInitiator;
    @Mock private UserAssociationStorage userAssociationStorage;

    private Scheduler scheduler = Schedulers.trampoline();
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

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Single.just(
                followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE,
                                                  Consts.NOT_SET)).thenReturn(Single.just(Collections.emptyList()),
                                                                              Single.just(firstPage));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Single.just(TestSyncJobResults.successWithChange()));
        when(syncInitiator.batchSyncUsers(followingsUrn)).thenReturn(Single.just(SYNC_JOB_RESULT));

        operations.followings().test().assertValue(firstPage);
    }

    @Test
    public void syncAndLoadEmptyFollowingsResultsWithEmptyResults() {
        when(userAssociationStorage.followedUserUrns(PAGE_SIZE,
                                                     Consts.NOT_SET)).thenReturn(Single.just(Collections.emptyList()));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Single.just(TestSyncJobResults.successWithChange()));

        operations.followings().test().assertValue(List::isEmpty);
    }

    @Test
    public void pagedFollowingsReturnsFollowingsFromStorage() {
        final List<Following> pageOfFollowings = createPageOfFollowings(2);
        final List<Urn> urns = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));
        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Single.just(urns));
        when(userAssociationStorage.followedUsers(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Single.just(
                pageOfFollowings));
        when(syncInitiator.batchSyncUsers(urns)).thenReturn(Single.just(SYNC_JOB_RESULT));

        operations.followings().test().assertValue(pageOfFollowings);
    }

    @Test
    public void followingsPagerLoadsNextPageUsingPositionOfLastItemOfPreviousPage() {
        final List<Following> firstPage = createPageOfFollowings(PAGE_SIZE);
        final List<Following> secondPage = createPageOfFollowings(1);
        final List<Urn> followingsUrn = pageOfUrns(firstPage);
        final long position = firstPage.get(PAGE_SIZE - 1).userAssociation().position();

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, position)).thenReturn(Single.just(followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE, position)).thenReturn(Single.just(secondPage));

        when(syncInitiator.batchSyncUsers(followingsUrn)).thenReturn(Single.just(SYNC_JOB_RESULT));

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
        final List<Following> pageOfFollowings = createPageOfFollowings(2);
        final List<Urn> followingsUrn = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Single.just(
                followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Single.just(
                pageOfFollowings));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Single.just(TestSyncJobResults.successWithChange()));
        when(syncInitiator.batchSyncUsers(followingsUrn)).thenReturn(Single.just(SYNC_JOB_RESULT));

        operations.updatedFollowings().test().assertValue(pageOfFollowings);
    }

    @Test
    public void shouldLoadLastPublicPostedTrack() {
        LastPostedTrack trackOpt = PlayableFixtures.expectedLastPostedTrackForPostsScreen();
        when(postStorage.loadLastPublicPostedTrack()).thenReturn(io.reactivex.Observable.just(trackOpt));

        operations.lastPublicPostedTrack().test().assertValue(trackOpt);
    }

    @Test
    public void returnsListOfFollowingsUrns() {
        final UserAssociation userAssociation1 = createUserAssociation(Urn.forUser(123L));
        final UserAssociation userAssociation2 = createUserAssociation(Urn.forUser(124L));
        final List<UserAssociation> followingsUrn = Arrays.asList(userAssociation1, userAssociation2);

        when(userAssociationStorage.followedUserAssociations()).thenReturn(Single.just(
                followingsUrn));

        operations.followingsUserAssociations().test().assertComplete().assertValue(followingsUrn);

        verify(syncInitiatorBridge, never()).refreshFollowings();
    }

    @Test
    public void syncsWhenStoredFollowingsListEmpty() {
        when(userAssociationStorage.followedUserAssociations()).thenReturn(Single.just(Collections.emptyList()));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Single.just(TestSyncJobResults.successWithChange()));

        operations.followingsUserAssociations().test().assertComplete().assertValue(Collections.emptyList());

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
