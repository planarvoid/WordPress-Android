package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class AdPageListenerTest {

    private AdPageListener listener;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    @Before
    public void setUp() throws Exception {
        listener = new AdPageListener(Robolectric.application,
                 playSessionStateProvider,
                 playbackOperations,
                 playQueueManager,
                 eventBus);
    }

    @Test
    public void onClickThroughShouldOpenUrlWhenCurrentTrackIsAudioAd() throws CreateModelException {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123));
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123));
        when(playQueueManager.getAudioAd()).thenReturn(audioAd);

        listener.onClickThrough();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Intent.ACTION_VIEW);
        expect(intent.getData()).toBe(audioAd.get(AdProperty.CLICK_THROUGH_LINK));
    }

    @Test
    public void onClickThroughShouldPublishUIEventForAudioAdClick() {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123));
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(456));
        when(playQueueManager.getAudioAd()).thenReturn(audioAd);

        listener.onClickThrough();

        final UIEvent uiEvent = eventBus.lastEventOn(EventQueue.UI);
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.AUDIO_AD_CLICK);
        expect(uiEvent.getAttributes().get("ad_track_urn")).toEqual(Urn.forTrack(123).toString());
    }

    @Test
    public void onTogglePlayEmitsPauseEventWhenWasPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        listener.onTogglePlay();

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event).toEqual(PlayControlEvent.pause(PlayControlEvent.SOURCE_FULL_PLAYER));
    }
}