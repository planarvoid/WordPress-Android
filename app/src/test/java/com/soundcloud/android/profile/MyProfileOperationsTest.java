package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.MyProfileOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.users.UserAssociationProperty;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
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
    private TestSubscriber<List<PropertySet>> subscriber;

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
        final List<PropertySet> firstPage = createPageOfFollowings(PAGE_SIZE);
        final List<Urn> followingsUrn = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(
                followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE,
                                                  Consts.NOT_SET)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()),
                                                                              Observable.just(firstPage));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Observable.<Void>just(null));
        when(syncInitiator.batchSyncUsers(followingsUrn)).thenReturn(Observable.just(SyncJobResult.success("success",
                                                                                                            true)));

        operations.pagedFollowings().subscribe(subscriber);

        subscriber.assertValue(firstPage);
    }

    @Test
    public void syncAndLoadEmptyFollowingsResultsWithEmptyResults() {
        when(userAssociationStorage.followedUserUrns(PAGE_SIZE,
                                                     Consts.NOT_SET)).thenReturn(Observable.just(Collections.<Urn>emptyList()));
        when(userAssociationStorage.followedUsers(PAGE_SIZE,
                                                  Consts.NOT_SET)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Observable.<Void>just(null));

        operations.pagedFollowings().subscribe(subscriber);

        subscriber.assertValue(Collections.<PropertySet>emptyList());
    }

    @Test
    public void pagedFollowingsReturnsFollowingsFromStorage() {
        final List<PropertySet> pageOfFollowings = createPageOfFollowings(2);
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
        final List<PropertySet> firstPage = createPageOfFollowings(PAGE_SIZE);
        final List<PropertySet> secondPage = createPageOfFollowings(1);
        final List<Urn> followingsUrn = pageOfUrns(firstPage);
        final long position = firstPage.get(PAGE_SIZE - 1).get(UserAssociationProperty.POSITION);

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, position)).thenReturn(Observable.just(followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE, position)).thenReturn(Observable.just(secondPage));

        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Observable.<Void>empty());
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
        final List<PropertySet> pageOfFollowings1 = createPageOfFollowings(2);
        final List<Urn> followingsUrn = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(
                followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(
                pageOfFollowings1));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(Observable.<Void>just(null));
        when(syncInitiator.batchSyncUsers(followingsUrn)).thenReturn(Observable.just(SyncJobResult.success("success",
                                                                                                           true)));
        final List<PropertySet> pageOfFollowings = pageOfFollowings1;

        operations.updatedFollowings().subscribe(subscriber);

        subscriber.assertValue(pageOfFollowings);
    }

    @Test
    public void shouldLoadLastPublicPostedTrack() {
        PropertySet trackOpt = TestPropertySets.expectedPostedTrackForPostsScreen();
        when(postStorage.loadLastPublicPostedTrack()).thenReturn(Observable.just(trackOpt));
        TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();

        operations.lastPublicPostedTrack().subscribe(subscriber);

        subscriber.assertValue(trackOpt);
    }

    @Test
    public void returnsListOfFollowingsUrns() {
        TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();
        int numberOfFollowings = 2;
        final List<PropertySet> pageOfFollowings = createPageOfFollowings(numberOfFollowings);
        final List<Urn> followingsUrn = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));

        when(userAssociationStorage.followedUserUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(
                followingsUrn));
        when(userAssociationStorage.followedUsers(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(
                pageOfFollowings));
        when(syncInitiatorBridge.refreshFollowings()).thenReturn(SHOULD_NEVER_SYNC);

        operations.followingsUrns().subscribe(subscriber);

        subscriber.assertValue(followingsUrn);
    }

    private List<PropertySet> createPageOfFollowings(int size) {
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            page.add(TestPropertySets.expectedFollowingForFollowingsScreen(i));
        }
        return page;
    }

    private List<Urn> pageOfUrns(List<PropertySet> propertySets) {
        List<Urn> page = new ArrayList<>(propertySets.size());
        for (PropertySet propertySet : propertySets) {
            page.add(propertySet.get(UserProperty.URN));
        }
        return page;
    }
}
