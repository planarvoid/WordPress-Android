package com.soundcloud.android.associations;

import static com.soundcloud.android.associations.UpdateFollowingCommand.UpdateFollowingParams;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.rx.eventbus.EventBusV2;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FollowingOperationsTest {

    private static final int FOLLOWER_COUNT = 2;
    private FollowingOperations operations;

    @Mock private NewSyncOperations syncOperations;
    @Mock private UpdateFollowingCommand updateFollowingCommand;
    @Mock private UserAssociationStorage userAssociationStorage;
    @Mock private EventBusV2 mockEventBus;
    @Captor private ArgumentCaptor<UpdateFollowingParams> commandParamsCaptor;
    @Captor private ArgumentCaptor<FollowingStatusEvent> followingChangedEventArgumentCaptor;

    private TestEventBusV2 eventBus = new TestEventBusV2();
    private Scheduler scheduler = Schedulers.trampoline();
    private Urn targetUrn = Urn.forUser(123);

    @Before
    public void setUp() throws Exception {
        operations = new FollowingOperations(eventBus,
                                             updateFollowingCommand,
                                             scheduler,
                                             userAssociationStorage,
                                             ModelFixtures.entityItemCreator(),
                                             syncOperations);

        when(updateFollowingCommand.toSingle(any(UpdateFollowingParams.class))).thenReturn(Single.just(FOLLOWER_COUNT));
    }

    @Test
    public void toggleFollowingStoresThenSyncThenEmitsChangeSet() {
        final FollowingOperations ops = new FollowingOperations(mockEventBus,
                                                                updateFollowingCommand,
                                                                scheduler,
                                                                userAssociationStorage,
                                                                ModelFixtures.entityItemCreator(),
                                                                syncOperations);

        when(syncOperations.failSafeSync(Syncable.MY_FOLLOWINGS)).thenReturn(Single.just(SyncResult.synced()));

        ops.toggleFollowing(targetUrn, true).test();

        final InOrder inOrder = Mockito.inOrder(updateFollowingCommand, syncOperations, mockEventBus);
        inOrder.verify(updateFollowingCommand).toSingle(commandParamsCaptor.capture());
        inOrder.verify(syncOperations).failSafeSync(Syncable.MY_FOLLOWINGS);
        inOrder.verify(mockEventBus).publish(eq(EventQueue.FOLLOWING_CHANGED), followingChangedEventArgumentCaptor.capture());

        final UpdateFollowingParams params = commandParamsCaptor.getValue();
        assertThat(params.following).isTrue();
        assertThat(params.targetUrn).isEqualTo(targetUrn);

        final FollowingStatusEvent event = followingChangedEventArgumentCaptor.getValue();
        assertThat(event).isEqualTo(FollowingStatusEvent.createFollowed(targetUrn, FOLLOWER_COUNT));
    }

    @Test
    public void onUserFollowedEmitsFollowedUser() {
        final TestObserver<UserItem> testObserver = operations.populatedOnUserFollowed().test();

        final FollowingStatusEvent event = FollowingStatusEvent.createFollowed(targetUrn, FOLLOWER_COUNT);
        final User following = ModelFixtures.user();
        when(userAssociationStorage.followedUser(targetUrn)).thenReturn(Maybe.just(following));

        eventBus.publish(EventQueue.FOLLOWING_CHANGED, event);

        testObserver.assertValues(ModelFixtures.userItem(following));
    }

    @Test
    public void onUserUnFollowedEmitsUrnOfUnfollowedUser() {
        final TestObserver<Urn> subscriber = operations.onUserUnfollowed().test();

        final FollowingStatusEvent event = FollowingStatusEvent.createUnfollowed(targetUrn, FOLLOWER_COUNT);
        eventBus.publish(EventQueue.FOLLOWING_CHANGED, event);

        subscriber.assertValues(targetUrn);
    }

}
