package com.soundcloud.android.associations;

import static com.soundcloud.android.associations.UpdateFollowingCommand.UpdateFollowingParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class FollowingOperationsTest extends AndroidUnitTest {

    private static final int FOLLOWER_COUNT = 2;
    private FollowingOperations operations;

    @Mock private SyncOperations syncOperations;
    @Mock private UpdateFollowingCommand updateFollowingCommand;
    @Mock private UserAssociationStorage userAssociationStorage;
    @Mock private EventBus mockEventBus;
    @Captor private ArgumentCaptor<UpdateFollowingParams> commandParamsCaptor;
    @Captor private ArgumentCaptor<FollowingStatusEvent> followingChangedEventArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<UserItem> subscriber = new TestSubscriber<>();
    private TestSubscriber<FollowingStatusEvent> followingStatusTestSubscriber = new TestSubscriber<>();
    private Urn targetUrn = Urn.forUser(123);

    @Before
    public void setUp() throws Exception {
        operations = new FollowingOperations(eventBus,
                                             updateFollowingCommand,
                                             scheduler,
                                             userAssociationStorage,
                                             syncOperations);

        when(updateFollowingCommand.toObservable(any(UpdateFollowingParams.class))).thenReturn(Observable.just(FOLLOWER_COUNT));
    }

    @Test
    public void toggleFollowingStoresThenSyncThenEmitsChangeSet() {
        final FollowingOperations ops = new FollowingOperations(mockEventBus,
                                             updateFollowingCommand,
                                             scheduler,
                                             userAssociationStorage,
                                             syncOperations);

        when(syncOperations.failSafeSync(Syncable.MY_FOLLOWINGS)).thenReturn(Observable.just(SyncOperations.Result.SYNCED));

        ops.toggleFollowing(targetUrn, true).subscribe(followingStatusTestSubscriber);

        final InOrder inOrder = Mockito.inOrder(updateFollowingCommand, syncOperations, mockEventBus);
        inOrder.verify(updateFollowingCommand).toObservable(commandParamsCaptor.capture());
        inOrder.verify(syncOperations).failSafeSync(Syncable.MY_FOLLOWINGS);
        inOrder.verify(mockEventBus).publish(eq(EventQueue.FOLLOWING_CHANGED), followingChangedEventArgumentCaptor.capture());

        final UpdateFollowingParams params = commandParamsCaptor.getValue();
        assertThat(params.following).isTrue();
        assertThat(params.targetUrn).isEqualTo(targetUrn);

        final FollowingStatusEvent event = followingChangedEventArgumentCaptor.getValue();
        assertThat(event).isEqualTo(FollowingStatusEvent.createFollowed(targetUrn, FOLLOWER_COUNT));
    }

    @Test
    public void onUserFollowedEmptsFollowedUser() {
        operations.populatedOnUserFollowed().subscribe(subscriber);

        final FollowingStatusEvent event = FollowingStatusEvent.createFollowed(targetUrn, FOLLOWER_COUNT);
        final UserItem following = UserItem.create(Urn.NOT_SET, "", Optional.absent(), Optional.absent(), 0, false);
        when(userAssociationStorage.followedUser(targetUrn)).thenReturn(Observable.just(following));

        eventBus.publish(EventQueue.FOLLOWING_CHANGED, event);

        subscriber.assertValues(following);
    }

    @Test
    public void onUserUnFollowedEmitsUrnOfUnfollowedUser() {
        final TestSubscriber<Urn> subscriber = new TestSubscriber<>();
        operations.onUserUnfollowed().subscribe(subscriber);

        final FollowingStatusEvent event = FollowingStatusEvent.createUnfollowed(targetUrn, FOLLOWER_COUNT);
        eventBus.publish(EventQueue.FOLLOWING_CHANGED, event);

        subscriber.assertValues(targetUrn);
    }

}
