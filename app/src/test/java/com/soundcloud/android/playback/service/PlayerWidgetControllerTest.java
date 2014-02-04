package com.soundcloud.android.playback.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus2;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.EventExpectation;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlayerWidgetControllerTest {

    private PlayerWidgetController controller;

    @Mock
    private Context context;
    @Mock
    private PlaybackStateProvider playbackStateProvider;
    @Mock
    private PlayerAppWidgetProvider widgetProvider;
    @Mock
    private EventBus2 eventBus;

    @Before
    public void setUp() throws Exception {
        when(context.getApplicationContext()).thenReturn(context);
        controller = new PlayerWidgetController(context, playbackStateProvider, widgetProvider, eventBus);
    }

    @Test
    public void shouldPerformUpdateOnWidgetWhenChangedTrackIsCurrentlyPlayingTrack() {
        final Track currentTrack = new Track(1L);
        when(playbackStateProvider.getCurrentTrackId()).thenReturn(currentTrack.getId());
        PlayableChangedEvent event = PlayableChangedEvent.create(currentTrack);

        EventExpectation eventExpectation = EventExpectation.on(eventBus).withQueue(EventQueue.PLAYABLE_CHANGED);
        controller.subscribe();
        eventExpectation.verifyObserverSubscribed().publish(event);

        verify(widgetProvider).performUpdate(context, currentTrack, false);
    }

    @Test
    public void shouldNotPerformUpdateOnWidgetWhenChangedTrackIsNotCurrentlyPlayingTrack() {
        final Track currentTrack = new Track(1L);
        when(playbackStateProvider.getCurrentTrackId()).thenReturn(2L);
        PlayableChangedEvent event = PlayableChangedEvent.create(currentTrack);

        EventExpectation eventExpectation = EventExpectation.on(eventBus).withQueue(EventQueue.PLAYABLE_CHANGED);
        controller.subscribe();
        eventExpectation.verifyObserverSubscribed().publish(event);

        verifyZeroInteractions(widgetProvider);
    }
}
