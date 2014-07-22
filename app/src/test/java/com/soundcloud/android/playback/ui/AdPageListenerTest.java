package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;
import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class AdPageListenerTest {

    private AdPageListener listener;

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        listener = new AdPageListener(Robolectric.application,
                 playbackOperations,
                 playQueueManager,
                 eventBus);
    }

    @Test
    public void onClickThroughShouldOpenUrlWhenCurrentTrackIsAudioAd() throws CreateModelException {
        Uri uri = Uri.parse("http://brand.com");
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getAudioAd()).thenReturn(PropertySet.create().put(AdProperty.CLICK_THROUGH_LINK, uri));

        listener.onClickThrough();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Intent.ACTION_VIEW);
        expect(intent.getData()).toBe(uri);
    }

    @Test
    public void onClickThroughShouldNotOpenUrlWhenCurrentTrackIsNotAnAudioAd() throws CreateModelException {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(false);

        listener.onClickThrough();

        expect(Robolectric.getShadowApplication().getNextStartedActivity()).toBeNull();
    }
}