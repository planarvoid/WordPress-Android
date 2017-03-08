package com.soundcloud.android.ads;


import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.AdSessionAnalyticsDispatcher;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class InlayAdAnalyticsControllerTest extends AndroidUnitTest {

    private static final VideoAd VIDEO_AD = AdFixtures.getInlayVideoAd(1L);

    @Mock private AdSessionAnalyticsDispatcher adDispatcher;

    private InlayAdAnalyticsController analyticsController;

    @Before
    public void setUp() {
        analyticsController = new InlayAdAnalyticsController(adDispatcher);
    }

    @Test
    public void onStateChangeForwardsEventToAdDispatcherAndSetsAdMetaData() {
        final PlayStateEvent event = TestPlayStates.playing(VIDEO_AD.getAdUrn());
        final ArgumentCaptor<TrackSourceInfo> trackSourceInfo = ArgumentCaptor.forClass(TrackSourceInfo.class);
        analyticsController.onStateTransition(Screen.STREAM, false, VIDEO_AD, event);

        verify(adDispatcher).setAdMetadata(eq(VIDEO_AD), trackSourceInfo.capture());
        verify(adDispatcher).onPlayTransition(event, true);
        assertThat(trackSourceInfo.getValue().getOriginScreen()).isEqualTo("stream:main");
        assertThat(trackSourceInfo.getValue().getIsUserTriggered()).isFalse();
    }

    @Test
    public void onStateChangeSetsAdMetaDataEvenIfNotNewItem() {
        final PlayStateEvent event = TestPlayStates.playing(VIDEO_AD.getAdUrn());

        analyticsController.onStateTransition(Screen.STREAM, false, VIDEO_AD, event);
        analyticsController.onStateTransition(Screen.STREAM, false, VIDEO_AD, event);

        verify(adDispatcher, times(2)).setAdMetadata(eq(VIDEO_AD), any(TrackSourceInfo.class));
    }

    @Test
    public void onProgressEventWontForwardEventToAdDispatcherIfStateEventForItWasNeverSent() {
        final PlaybackProgressEvent event = PlaybackProgressEvent.create(new PlaybackProgress(12, 30, VIDEO_AD.getAdUrn()), VIDEO_AD.getAdUrn());
        analyticsController.onProgressEvent(VIDEO_AD, event);

        verify(adDispatcher, never()).onProgressEvent(event);
    }
}
