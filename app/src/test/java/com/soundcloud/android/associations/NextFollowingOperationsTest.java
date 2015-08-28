package com.soundcloud.android.associations;

import static com.soundcloud.android.associations.UpdateFollowingCommand.UpdateFollowingParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class NextFollowingOperationsTest extends AndroidUnitTest {

    public static final int FOLLOWER_COUNT = 2;
    private NextFollowingOperations operations;

    @Mock private SyncInitiator syncInitiator;
    @Mock private UpdateFollowingCommand updateFollowingCommand;
    @Mock private UserAssociationStorage userAssociationStorage;
    @Captor private ArgumentCaptor<UpdateFollowingParams> commandParamsCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();
    private Urn targetUrn = Urn.forUser(123);

    @Before
    public void setUp() throws Exception {
        operations = new NextFollowingOperations(
                syncInitiator, eventBus, updateFollowingCommand, scheduler, userAssociationStorage);

        when(updateFollowingCommand.toObservable(any(UpdateFollowingParams.class))).thenReturn(Observable.just(5));
    }

    @Test
    public void toggleFollowingEmitsEntityChangeSet() {
        operations.toggleFollowing(targetUrn, true).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).containsExactly(TestPropertySets.followingEntityChangeSet(targetUrn, 5, true));
    }

    @Test
    public void toggleFollowingPushesFollowingsViaSyncInitiator() {
        operations.toggleFollowing(targetUrn, true).subscribe(subscriber);

        verify(syncInitiator).pushFollowingsToApi();
    }

    @Test
    public void toggleFollowingUpdateUserAssociations() {
        operations.toggleFollowing(targetUrn, true).subscribe(subscriber);

        verify(updateFollowingCommand).toObservable(commandParamsCaptor.capture());
        assertThat(commandParamsCaptor.getValue().following).isTrue();
        assertThat(commandParamsCaptor.getValue().targetUrn).isEqualTo(targetUrn);
    }

    @Test
    public void onUserFollowedEmptsFollowedUser() {
        operations.onUserFollowed().subscribe(subscriber);

        final EntityStateChangedEvent event = EntityStateChangedEvent.fromFollowing(getFollowingChangeSet(true));
        final PropertySet following = PropertySet.create();
        when(userAssociationStorage.loadFollowing(targetUrn)).thenReturn(Observable.just(following));

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
