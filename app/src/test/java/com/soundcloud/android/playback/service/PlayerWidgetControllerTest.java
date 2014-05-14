package com.soundcloud.android.playback.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlayerWidgetControllerTest {

    private PlayerWidgetController controller;
    private EventMonitor eventMonitor;

    @Mock
    private Context context;
    @Mock
    private PlaybackStateProvider playbackStateProvider;
    @Mock
    private PlayerAppWidgetProvider widgetProvider;
    @Mock
    private SoundAssociationOperations soundAssociationOperations;
    @Mock
    private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        when(context.getApplicationContext()).thenReturn(context);
        controller = new PlayerWidgetController(context, playbackStateProvider, widgetProvider,
                soundAssociationOperations, eventBus);
        eventMonitor = EventMonitor.on(eventBus);
    }

    @Test
    public void shouldPerformUpdateOnWidgetWhenChangedTrackIsCurrentlyPlayingTrack() {
        final Track currentTrack = new Track(1L);
        when(playbackStateProvider.getCurrentTrackId()).thenReturn(currentTrack.getId());
        PlayableChangedEvent event = PlayableChangedEvent.create(currentTrack);

        controller.subscribe();

        eventMonitor.publish(EventQueue.PLAYABLE_CHANGED, event);
        verify(widgetProvider).performUpdate(context, currentTrack, false);
    }

    @Test
    public void shouldNotPerformUpdateOnWidgetWhenChangedTrackIsNotCurrentlyPlayingTrack() {
        final Track currentTrack = new Track(1L);
        when(playbackStateProvider.getCurrentTrackId()).thenReturn(2L);
        PlayableChangedEvent event = PlayableChangedEvent.create(currentTrack);

        controller.subscribe();

        eventMonitor.publish(EventQueue.PLAYABLE_CHANGED, event);
        verifyZeroInteractions(widgetProvider);
    }

    @Test
    public void callsResetActionWhenCurrentUserChangedEventReceivedForUserRemoved() {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forLogout();

        controller.subscribe();

        eventMonitor.publish(EventQueue.CURRENT_USER_CHANGED, event);
        verify(widgetProvider).reset(context);
    }

    @Test
    public void doesNotInteractWithProviderWhenCurrentUserChangedEventReceivedForUserUpdated() {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(new User(1));

        controller.subscribe();

        eventMonitor.publish(EventQueue.CURRENT_USER_CHANGED, event);
        verifyZeroInteractions(widgetProvider);
    }
}
