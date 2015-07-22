package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.app.Activity;
import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class AdPageListenerTest {

    private AdPageListener listener;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private AdsOperations adsOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private WhyAdsDialogPresenter whyAdsPresenter;

    @Mock private Activity activity;

    @Before
    public void setUp() throws Exception {
        listener = new AdPageListener(Robolectric.application,
                 playSessionStateProvider, playbackOperations, playQueueManager,
                 eventBus, adsOperations, accountOperations, whyAdsPresenter);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
    }

    @Test
    public void onClickThroughShouldOpenUrlWhenCurrentTrackIsAudioAd() throws CreateModelException {
        when(adsOperations.getMonetizableTrackMetaData()).thenReturn(PropertySet.create());
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123));
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123));
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAd);

        listener.onClickThrough();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Intent.ACTION_VIEW);
        expect(intent.getData()).toBe(audioAd.get(AdProperty.CLICK_THROUGH_LINK));
    }

    @Test
    public void onClickThroughShouldPublishUIEventForAudioAdClick() {
        when(adsOperations.getMonetizableTrackMetaData()).thenReturn(PropertySet.create());
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123));
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(456));
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAd);

        listener.onClickThrough();

        final UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_AUDIO_AD_CLICK);
        expect(uiEvent.getAttributes().get("ad_track_urn")).toEqual(Urn.forTrack(123).toString());
    }

    @Test
    public void onClickThroughShouldSetMenetizableTrackMetaAdClicked() {
        final PropertySet monetizableProperties = PropertySet.create();
        when(adsOperations.getMonetizableTrackMetaData()).thenReturn(monetizableProperties);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123));
        when(playQueueManager.getCurrentMetaData()).thenReturn(TestPropertySets.audioAdProperties(Urn.forTrack(456)));

        listener.onClickThrough();

        expect(monetizableProperties.get(LeaveBehindProperty.META_AD_CLICKED)).toBeTrue();
    }

    @Test
    public void onNextEmitsSkipEventWithFullPlayer() {
        listener.onNext();

        PlayControlEvent expectedEvent = PlayControlEvent.skip(PlayControlEvent.SOURCE_FULL_PLAYER);
        expect(eventBus.lastEventOn(EventQueue.TRACKING)).toEqual(expectedEvent);
    }

    @Test
    public void onPreviousEmitsPreviousEventWithFullPlayer() {
        listener.onPrevious();

        PlayControlEvent expectedEvent = PlayControlEvent.previous(PlayControlEvent.SOURCE_FULL_PLAYER);
        expect(eventBus.lastEventOn(EventQueue.TRACKING)).toEqual(expectedEvent);
    }

    @Test
    public void skipAdEmitsSkipADEventWithFullPlayer() {
        listener.onSkipAd();

        PlayControlEvent expectedEvent = PlayControlEvent.skipAd();
        expect(eventBus.lastEventOn(EventQueue.TRACKING)).toEqual(expectedEvent);
    }

    @Test
    public void onAboutAdsShowsDialog() {
        listener.onAboutAds(activity);
        verify(whyAdsPresenter).show(activity);
    }

}
