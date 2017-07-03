package com.soundcloud.android.playback.widget;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackActionSource;
import com.soundcloud.android.playback.PlayerInteractionsTracker;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.playback.external.PlaybackActionController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.content.Intent;

@RunWith(MockitoJUnitRunner.class)
public class WidgetPlaybackActionReceiverTest {

    @Mock PlaybackActionController playbackActionController;
    @Mock PlayerInteractionsTracker playerInteractionsTracker;
    @Mock Intent intent;

    private WidgetPlaybackActionReceiver.Controller controller;

    @Before
    public void setUp() throws Exception {
        controller = new WidgetPlaybackActionReceiver.Controller(playbackActionController, playerInteractionsTracker);
    }

    @Test
    public void intentWithActionNextShouldTrackClickForward() {
        when(intent.getAction()).thenReturn(PlaybackAction.NEXT);

        controller.handleIntent(intent);

        verify(playerInteractionsTracker).clickForward(PlaybackActionSource.WIDGET);
    }

    @Test
    public void intentWithActionPreviousShouldTrackClickBackward() {
        when(intent.getAction()).thenReturn(PlaybackAction.PREVIOUS);

        controller.handleIntent(intent);

        verify(playerInteractionsTracker).clickBackward(PlaybackActionSource.WIDGET);
    }

    @Test
    public void intentWithUnsupportedActionShouldNotTrack() {
        when(intent.getAction()).thenReturn(PlaybackAction.PLAY);

        controller.handleIntent(intent);

        verifyZeroInteractions(playerInteractionsTracker);
    }

    @Test
    public void shouldPassTheActionToPlayActionController() {
        when(intent.getAction()).thenReturn("whatever");

        controller.handleIntent(intent);

        verify(playbackActionController).handleAction("whatever", PlaybackActionSource.WIDGET);
    }
}
