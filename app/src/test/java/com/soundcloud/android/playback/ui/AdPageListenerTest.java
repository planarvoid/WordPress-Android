package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.WhyAdsDialogPresenter;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableTrackingKeys;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
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
import android.content.Context;
import android.net.Uri;

public class AdPageListenerTest extends AndroidUnitTest {

    private AdPageListener listener;
    private TestEventBus eventBus = new TestEventBus();
    private AudioAd adData;

    @Mock private PlaySessionController playSessionController;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private WhyAdsDialogPresenter whyAdsPresenter;
    @Mock private Activity activity;
    @Mock private Navigator navigator;

    @Before
    public void setUp() throws Exception {
        listener = new AdPageListener(navigator, playSessionController, playQueueManager,
                                      eventBus, adsOperations, whyAdsPresenter);

        adData = AdFixtures.getAudioAd(Urn.forTrack(123L));

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createAudioAd(adData));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(adData));
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>absent());
    }

    @Test
    public void onClickThroughShouldOpenUrlForAudioAd() throws CreateModelException {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);

        listener.onClickThrough(context());

        verify(navigator).openAdClickthrough(context(), Uri.parse(adData.getClickThroughUrl().get()));
    }

    @Test
    public void onClickThroughShouldOpenUrlForVideoAd() throws CreateModelException {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(videoAd));

        listener.onClickThrough(context());

        verify(navigator).openAdClickthrough(context(), Uri.parse(videoAd.getClickThroughUrl()));
    }

    @Test
    public void onClickThroughShouldPublishUIEventForAudioAdClick() {
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);

        listener.onClickThrough(context());

        final UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_AD_CLICKTHROUGH);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "869").toString());
    }

    @Test
    public void onClickThroughShouldPublishUIEventForVideoAdClick() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(videoAd));
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>absent());

        listener.onClickThrough(context());

        final UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_AD_CLICKTHROUGH);
    }

    @Test
    public void onClickThroughShouldSetMonetizableTrackMetaAdClicked() {
        final LeaveBehindAd monetizableLeaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(321L));
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.<AdData>of(monetizableLeaveBehindAd));

        listener.onClickThrough(context());

        assertThat(monetizableLeaveBehindAd.isMetaAdClicked()).isTrue();
    }

    @Test
    public void onClickthroughForUserProfileDeeplinkShouldCloserPlayer() {
        final AudioAd userAudioAd = AdFixtures.getAudioAdWithCustomClickthrough("soundcloud://users/42", Urn.forTrack(123L));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(userAudioAd));

        listener.onClickThrough(context());

        PlayerUICommand event = eventBus.firstEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isCollapse()).isTrue();
    }

    @Test
    public void onClickthroughForPlaylistDeeplinkShouldClosePlayer() {
        final AudioAd playlistAudioAd = AdFixtures.getAudioAdWithCustomClickthrough("soundcloud://playlists/42", Urn.forTrack(123L));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(playlistAudioAd));

        listener.onClickThrough(context());

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isCollapse()).isTrue();
    }

    @Test
    public void onClickthroughforUserProfileDeeplinkshouldStartProfileActivityAfterPlayerUICollapsed() {
        final AudioAd userAudioAd = AdFixtures.getAudioAdWithCustomClickthrough("soundcloud://users/42", Urn.forTrack(123L));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(userAudioAd));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createAudioAd(userAudioAd));

        listener.onClickThrough(context());
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(playQueueManager).moveToNextPlayableItem();
        verify(navigator).legacyOpenProfile(any(Context.class), eq(Urn.forUser(42L)));
    }

    @Test
    public void onClickthroughforPlaylistDeeplinkshouldStartPlaylistActivityAfterPlayerUICollapsed() {
        final AudioAd playlistAudioAd = AdFixtures.getAudioAdWithCustomClickthrough("soundcloud://playlists/42", Urn.forTrack(123L));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(playlistAudioAd));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createAudioAd(playlistAudioAd));
        when(playQueueManager.getScreenTag()).thenReturn("stream:main");

        listener.onClickThrough(context());
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(playQueueManager).moveToNextPlayableItem();
        verify(navigator).legacyOpenPlaylist(any(Context.class), eq(Urn.forPlaylist(42L)), eq(Screen.STREAM));
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
