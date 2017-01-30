package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;

public class ExpandPlayerSubscriberTest extends AndroidUnitTest {
    private ExpandPlayerSubscriber subscriber;

    private TestEventBus eventBus;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private MiniplayerExperiment miniplayerExperiment;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new ExpandPlayerSubscriber(eventBus, playbackToastHelper, miniplayerExperiment);
    }

    @Test
    public void expandsPlayerOnPlaybackResultSuccessAndExperimentSaysSo() {
        when(miniplayerExperiment.canExpandPlayer()).thenReturn(true);

        subscriber.onNext(PlaybackResult.success());

        Robolectric.flushForegroundThreadScheduler();
        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isExpand()).isTrue();
    }

    @Test
    public void doesNotExpandPlayerOnPlaybackResultSuccessAndExperimentSaysSo() {
        when(miniplayerExperiment.canExpandPlayer()).thenReturn(false);

        subscriber.onNext(PlaybackResult.success());

        Robolectric.flushForegroundThreadScheduler();
        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).isEmpty()).isTrue();
    }

    @Test
    public void showsToastOnPlaybackResultError() {
        PlaybackResult errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.UNSKIPPABLE);

        subscriber.onNext(errorResult);

        verify(playbackToastHelper).showToastOnPlaybackError(errorResult.getErrorReason());
    }

}
