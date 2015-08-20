package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.likes.UpdateLikeCommand.UpdateLikeParams;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class LikeOperationsTest {

    private LikeOperations operations;

    @Mock private UpdateLikeCommand updateLikeCommand;
    @Mock private SyncInitiator syncInitiator;
    @Mock private Action0 requestSystemSyncAction;
    @Captor private ArgumentCaptor<UpdateLikeParams> commandParamsCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<PropertySet> observer = new TestSubscriber<>();
    private Urn targetUrn = Urn.forTrack(123);

    @Before
    public void setUp() throws Exception {
        operations = new LikeOperations(
                updateLikeCommand,
                syncInitiator,
                eventBus,
                scheduler);
        when(updateLikeCommand.toObservable(any(UpdateLikeParams.class))).thenReturn(Observable.just(5));
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
    }

    @Test
    public void toggleLikeAddsNewLikeAndEmitsEntityChangeSet() {
        operations.toggleLike(targetUrn, true).subscribe(observer);

        verify(updateLikeCommand).toObservable(commandParamsCaptor.capture());
        expect(commandParamsCaptor.getValue().addLike).toBeTrue();
        expect(commandParamsCaptor.getValue().targetUrn).toEqual(targetUrn);
        expect(observer.getOnNextEvents()).toContainExactly(TestPropertySets.likedEntityChangeSet(targetUrn, 5));
    }

    @Test
    public void toggleLikeRemovesLikeAndEmitsEntityChangeSet() {
        operations.toggleLike(targetUrn, false).subscribe(observer);

        verify(updateLikeCommand).toObservable(commandParamsCaptor.capture());
        expect(commandParamsCaptor.getValue().addLike).toBeFalse();
        expect(commandParamsCaptor.getValue().targetUrn).toEqual(targetUrn);
        expect(observer.getOnNextEvents()).toContainExactly(TestPropertySets.unlikedEntityChangeSet(targetUrn, 5));
    }

    @Test
    public void togglingLikePublishesPlayableChangedEvent() {
        operations.toggleLike(targetUrn, true).subscribe(observer);

        EntityStateChangedEvent event = eventBus.firstEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getFirstUrn()).toEqual(targetUrn);
        expect(event.getNextChangeSet().contains(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(event.getNextChangeSet().contains(PlayableProperty.LIKES_COUNT)).toBeTrue();
    }

    @Test
    public void togglingLikeRequestsSystemSync() {
        operations.toggleLike(targetUrn, true).subscribe(observer);

        verify(requestSystemSyncAction).call();
    }

}