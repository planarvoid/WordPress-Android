package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;

public class AdPageListenerTest extends AndroidUnitTest {

    private AdPageListener listener;
    private TestEventBus eventBus = new TestEventBus();
    private PlayerAdData adData;

    @Mock private PlaySessionController playSessionController;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private WhyAdsDialogPresenter whyAdsPresenter;
    @Mock private Activity activity;
    @Mock private Navigator navigator;

    @Before
    public void setUp() throws Exception {
        listener = new AdPageListener(context(), navigator,
                                      playSessionController, playQueueManager,
                                      eventBus, adsOperations, accountOperations, whyAdsPresenter);

        adData = AdFixtures.getAudioAd(Urn.forTrack(123L));

        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L),
                                                                                                  adData));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(adData));
    }

    @Test
    public void onClickThroughShouldOpenUrlForAudioAd() throws CreateModelException {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>absent());

        listener.onClickThrough();

        final AudioAd audioAd = (AudioAd) adData;
        verify(navigator).openAdClickthrough(context(), audioAd.getClickThroughUrl().get());
    }

    @Test
    public void onClickThroughShouldOpenUrlForVideoAd() throws CreateModelException {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(videoAd));
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>absent());

        listener.onClickThrough();

        verify(navigator).openAdClickthrough(context(), videoAd.getClickThroughUrl());
    }

    @Test
    public void onClickThroughShouldPublishUIEventForAudioAdClick() {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>absent());

        listener.onClickThrough();

        final UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_AUDIO_AD_CLICK);
        assertThat(uiEvent.getAttributes().get("ad_track_urn")).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void onClickThroughShouldPublishUIEventForVideoAdClick() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(videoAd));
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>absent());

        listener.onClickThrough();

        final UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_VIDEO_AD_CLICKTHROUGH);
    }

    @Test
    public void onClickThroughShouldSetMonetizableTrackMetaAdClicked() {
        final LeaveBehindAd monetizableLeaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(321L));
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>of(monetizableLeaveBehindAd));

        listener.onClickThrough();

        assertThat(monetizableLeaveBehindAd.isMetaAdClicked()).isTrue();
    }

    @Test
    public void onLandscapeEmitsForceLandscapeUICommandEvent() {
        listener.onFullscreen();

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isForceLandscape()).isTrue();
    }

    @Test
    public void onLandscapeEmitsForcePortraitUICommandEvent() {
        listener.onShrink();

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isForcePortrait()).isTrue();
    }

    @Test
    public void onAboutAdsShowsDialog() {
        listener.onAboutAds(activity);
        verify(whyAdsPresenter).show(activity);
    }

}
