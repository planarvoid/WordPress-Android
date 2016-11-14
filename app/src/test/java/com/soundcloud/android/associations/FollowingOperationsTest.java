package com.soundcloud.android.associations;

import static com.soundcloud.android.associations.UpdateFollowingCommand.UpdateFollowingParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
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
    @Captor private ArgumentCaptor<EntityStateChangedEvent> entityStateChangedEventArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();
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

        ops.toggleFollowing(targetUrn, true).subscribe(subscriber);

        final InOrder inOrder = Mockito.inOrder(updateFollowingCommand, syncOperations, mockEventBus);
        inOrder.verify(updateFollowingCommand).toObservable(commandParamsCaptor.capture());
        inOrder.verify(syncOperations).failSafeSync(Syncable.MY_FOLLOWINGS);
        inOrder.verify(mockEventBus).publish(eq(EventQueue.ENTITY_STATE_CHANGED), entityStateChangedEventArgumentCaptor.capture());

        final UpdateFollowingParams params = commandParamsCaptor.getValue();
        assertThat(params.following).isTrue();
        assertThat(params.targetUrn).isEqualTo(targetUrn);

        final EntityStateChangedEvent event = entityStateChangedEventArgumentCaptor.getValue();
        assertThat(event.getChangeMap().get(targetUrn)).isEqualTo(getFollowingChangeSet(true));
        assertThat(event.isFollowingKind()).isTrue();
    }

    @Test
    public void onUserFollowedEmptsFollowedUser() {
        operations.populatedOnUserFollowed().subscribe(subscriber);

        final EntityStateChangedEvent event = EntityStateChangedEvent.fromFollowing(getFollowingChangeSet(true));
        final PropertySet following = PropertySet.create();
        when(userAssociationStorage.followedUser(targetUrn)).thenReturn(Observable.just(following));

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        subscriber.assertValues(following);
    }

    @Test
    public void onUserUnFollowedEmitsUrnOfUnfollowedUser() {
        final TestSubscriber<Urn> subscriber = new TestSubscriber<>();
        operations.onUserUnfollowed().subscribe(subscriber);

        final EntityStateChangedEvent event = EntityStateChangedEvent.fromFollowing(getFollowingChangeSet(false));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        subscriber.assertValues(targetUrn);
    }

    private PropertySet getFollowingChangeSet(boolean isFollowing) {
        return PropertySet.from(
                UserProperty.URN.bind(targetUrn),
                UserProperty.FOLLOWERS_COUNT.bind(FOLLOWER_COUNT),
                UserProperty.IS_FOLLOWED_BY_ME.bind(isFollowing));
    }
}
