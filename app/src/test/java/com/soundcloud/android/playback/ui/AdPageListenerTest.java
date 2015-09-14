package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Shadows;

import android.app.Activity;
import android.content.Intent;

public class AdPageListenerTest extends AndroidUnitTest {

    private AdPageListener listener;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaySessionController playSessionController;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private AdsOperations adsOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private WhyAdsDialogPresenter whyAdsPresenter;
    @Mock private Activity activity;

    @Before
    public void setUp() throws Exception {
        listener = new AdPageListener(context(),
                 playSessionStateProvider, playSessionController, playQueueManager,
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

        Intent intent = Shadows.shadowOf(context()).getShadowApplication().getNextStartedActivity();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData()).isSameAs(audioAd.get(AdProperty.CLICK_THROUGH_LINK));
    }

    @Test
    public void onClickThroughShouldPublishUIEventForAudioAdClick() {
        when(adsOperations.getMonetizableTrackMetaData()).thenReturn(PropertySet.create());
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123));
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(456));
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAd);

        listener.onClickThrough();

        final UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_AUDIO_AD_CLICK);
        assertThat(uiEvent.getAttributes().get("ad_track_urn")).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void onClickThroughShouldSetMenetizableTrackMetaAdClicked() {
        final PropertySet monetizableProperties = PropertySet.create();
        when(adsOperations.getMonetizableTrackMetaData()).thenReturn(monetizableProperties);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123));
        when(playQueueManager.getCurrentMetaData()).thenReturn(TestPropertySets.audioAdProperties(Urn.forTrack(456)));

        listener.onClickThrough();

        assertThat(monetizableProperties.get(LeaveBehindProperty.META_AD_CLICKED)).isTrue();
    }

    @Test
    public void onNextEmitsSkipEventWithFullPlayer() {
        listener.onNext();

        PlayControlEvent expectedEvent = PlayControlEvent.skip(PlayControlEvent.SOURCE_FULL_PLAYER);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(expectedEvent);
    }

    @Test
    public void onPreviousEmitsPreviousEventWithFullPlayer() {
        listener.onPrevious();

        PlayControlEvent expectedEvent = PlayControlEvent.previous(PlayControlEvent.SOURCE_FULL_PLAYER);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(expectedEvent);
    }

    @Test
    public void skipAdEmitsSkipADEventWithFullPlayer() {
        listener.onSkipAd();

        PlayControlEvent expectedEvent = PlayControlEvent.skipAd();
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(expectedEvent);
    }

    @Test
    public void onAboutAdsShowsDialog() {
        listener.onAboutAds(activity);
        verify(whyAdsPresenter).show(activity);
    }

}
