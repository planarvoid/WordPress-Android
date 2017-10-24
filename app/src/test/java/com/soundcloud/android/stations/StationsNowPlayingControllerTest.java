package com.soundcloud.android.stations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.view.adapters.PlayingTrackAware;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v4.app.Fragment;

@RunWith(MockitoJUnitRunner.class)
public class StationsNowPlayingControllerTest {
    private static final Urn COLLECTION_URN = Urn.forTrackStation(456L);
    private static final CurrentPlayQueueItemEvent EVENT = CurrentPlayQueueItemEvent
            .fromNewQueue(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), COLLECTION_URN, 0);

    private Fragment fragment = new Fragment();
    private StationsNowPlayingController controller;
    private TestEventBus eventBus;
    @Mock PlayingTrackAware adapter;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        controller = new StationsNowPlayingController(eventBus);
    }

    @Test
    public void isFunctionalWhenAdapterNotSet() {
        controller.onResume(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, EVENT);
    }

    @Test
    public void updateAdapterWhenAnEventIsFired() {
        controller.setAdapter(adapter);
        controller.onResume(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, EVENT);

        verify(adapter).updateNowPlaying(COLLECTION_URN);
    }

    @Test
    public void doesNotUpdateAdapterWhenOnResumeIsNotCalled() {
        controller.setAdapter(adapter);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, EVENT);

        verifyZeroInteractions(adapter);
    }

    @Test
    public void unSubscribeToQueueOnPause() {
        controller.setAdapter(adapter);
        controller.onResume(fragment);
        controller.onPause(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, EVENT);

        verifyZeroInteractions(adapter);
    }
}
