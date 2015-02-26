package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LikeOperationsTest {

    private LikeOperations operations;

    @Mock private Observer<List<PropertySet>> observer;
    @Mock private UpdateLikeCommand storeLikeCommand;
    @Mock private SyncInitiator syncInitiator;
    @Mock private Action0 requestSystemSyncAction;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        operations = new LikeOperations(
                storeLikeCommand,
                syncInitiator,
                eventBus,
                scheduler);
        when(storeLikeCommand.toObservable()).thenReturn(
                Observable.just(TestPropertySets.likedTrack(Urn.forTrack(123))));
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
    }

    @Test
    public void toggleLikeAddsNewLike() {
        final PropertySet track = TestPropertySets.unlikedTrack(Urn.forTrack(123L));
        operations.addLike(track).subscribe();

        verify(storeLikeCommand).toObservable();
        final PropertySet input = storeLikeCommand.getInput();
        expect(input.contains(LikeProperty.ADDED_AT)).toBeTrue();
        expect(input.contains(LikeProperty.CREATED_AT)).toBeTrue();
        expect(input.get(PlayableProperty.IS_LIKED)).toBeTrue();
    }

    @Test
    public void toggleLikeRemovesLike() {
        final PropertySet track = TestPropertySets.likedTrack(Urn.forTrack(123L));

        operations.removeLike(track).subscribe();

        verify(storeLikeCommand).toObservable();
        final PropertySet input = storeLikeCommand.getInput();
        expect(input.contains(LikeProperty.REMOVED_AT)).toBeTrue();
        expect(input.contains(LikeProperty.CREATED_AT)).toBeTrue();
        expect(input.get(PlayableProperty.IS_LIKED)).toBeFalse();
    }

    @Test
    public void togglingLikePublishesPlayableChangedEvent() {
        final PropertySet track = TestPropertySets.likedTrack(Urn.forTrack(123L));

        operations.addLike(track).subscribe();

        EntityStateChangedEvent event = eventBus.firstEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getNextUrn()).toEqual(track.get(PlayableProperty.URN));
        expect(event.getNextChangeSet().contains(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(event.getNextChangeSet().contains(PlayableProperty.LIKES_COUNT)).toBeTrue();
    }

    @Test
    public void togglingLikeRequestsSystemSync() {
        final PropertySet track = TestPropertySets.likedTrack(Urn.forTrack(123L));

        operations.addLike(track).subscribe();

        verify(requestSystemSyncAction).call();
    }

}