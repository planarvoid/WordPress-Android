package com.soundcloud.android.stations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.Fragment;

public class StationsNowPlayingControllerTest extends AndroidUnitTest {
    private static final Urn COLLECTION_URN = Urn.forTrackStation(456L);
    private static final CurrentPlayQueueTrackEvent EVENT = CurrentPlayQueueTrackEvent
            .fromNewQueue(Urn.forTrack(123L), COLLECTION_URN, 0);

    private Fragment fragment = new Fragment();
    private StationsNowPlayingController controller;
    private TestEventBus eventBus;
    @Mock StationsNowPlayingController.StationsNowPlayingAdapter adapter;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        controller = new StationsNowPlayingController(eventBus);
    }

    @Test
    public void isFunctionalWhenAdapterNotSet() {
        controller.onResume(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, EVENT);
    }

    @Test
    public void updateAdapterWhenAnEventIsFired() {
        controller.setAdapter(adapter);
        controller.onResume(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, EVENT);

        verify(adapter).updateNowPlaying(COLLECTION_URN);
    }

    @Test
    public void doesNotUpdateAdapterWhenOnResumeIsNotCalled() {
        controller.setAdapter(adapter);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, EVENT);

        verifyZeroInteractions(adapter);
    }

    @Test
    public void unSubscribeToQueueOnPause() {
        controller.setAdapter(adapter);
        controller.onResume(fragment);
        controller.onPause(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, EVENT);

        verifyZeroInteractions(adapter);
    }
}