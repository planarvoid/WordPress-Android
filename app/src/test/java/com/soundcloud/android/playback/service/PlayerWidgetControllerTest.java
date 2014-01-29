package com.soundcloud.android.playback.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
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

    @Before
    public void setUp() throws Exception {
        when(context.getApplicationContext()).thenReturn(context);
        controller = new PlayerWidgetController(context, playbackStateProvider, widgetProvider);
        controller.subscribe();
    }

    @After
    public void tearDown() throws Exception {
        controller.unsubscribe();
    }

    @Test
    public void shouldPerformUpdateOnWidgetWhenChangedTrackIsCurrentlyPlayingTrack() {
        final Track currentTrack = new Track(1L);
        when(playbackStateProvider.getCurrentTrackId()).thenReturn(currentTrack.getId());
        PlayableChangedEvent event = PlayableChangedEvent.create(currentTrack);

        EventBus.PLAYABLE_CHANGED.publish(event);

        verify(widgetProvider).performUpdate(context, currentTrack, false);
    }

    @Test
    public void shouldNotPerformUpdateOnWidgetWhenChangedTrackIsNotCurrentlyPlayingTrack() {
        final Track currentTrack = new Track(1L);
        when(playbackStateProvider.getCurrentTrackId()).thenReturn(2L);
        PlayableChangedEvent event = PlayableChangedEvent.create(currentTrack);

        EventBus.PLAYABLE_CHANGED.publish(event);

        verifyZeroInteractions(widgetProvider);
    }
}
