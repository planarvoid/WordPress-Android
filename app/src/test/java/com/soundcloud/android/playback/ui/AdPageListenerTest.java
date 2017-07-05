package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.WhyAdsDialogPresenter;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlayerInteractionsTracker;
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
import android.support.v7.app.AppCompatActivity;

public class AdPageListenerTest extends AndroidUnitTest {

    private AdPageListener listener;
    private TestEventBus eventBus = new TestEventBus();
    private AudioAd adData;

    @Mock private PlaySessionController playSessionController;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private WhyAdsDialogPresenter whyAdsPresenter;
    @Mock private Navigator navigator;
    @Mock private PlayerInteractionsTracker playerInteractionsTracker;

    @Before
    public void setUp() throws Exception {
        listener = new AdPageListener(playSessionController,
                                      playQueueManager,
                                      eventBus,
                                      adsOperations,
                                      whyAdsPresenter,
                                      navigator,
                                      playerInteractionsTracker);

        adData = AdFixtures.getAudioAd(Urn.forTrack(123L));

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createAudioAd(adData));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(adData));
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.absent());
    }

    @Test
    public void onClickThroughShouldOpenUrlForAudioAd() throws CreateModelException {
        Activity activity = activity();
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);

        listener.onClickThrough(activity);

        verify(navigator).navigateTo(activity, NavigationTarget.forAdClickthrough(adData.clickThroughUrl().get()));
    }

    @Test
    public void onClickThroughShouldOpenUrlForVideoAd() throws CreateModelException {
        Activity activity = activity();
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(videoAd));

        listener.onClickThrough(activity);

        verify(navigator).navigateTo(activity, NavigationTarget.forAdClickthrough(videoAd.clickThroughUrl()));
    }

    @Test
    public void onClickThroughShouldPublishUIEventForAudioAdClick() {
        Activity activity = activity();
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);

        listener.onClickThrough(activity);

        final UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.AD_CLICKTHROUGH);
        assertThat(uiEvent.adUrn().get()).isEqualTo(Urn.forAd("dfp", "869").toString());
    }

    @Test
    public void onClickThroughShouldPublishUIEventForVideoAdClick() {
        Activity activity = activity();
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(videoAd));
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.absent());

        listener.onClickThrough(activity);

        final UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.AD_CLICKTHROUGH);
    }

    @Test
    public void onClickThroughShouldSetMonetizableTrackMetaAdClicked() {
        Activity activity = activity();
        final LeaveBehindAd monetizableLeaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(321L));
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(adsOperations.getNextTrackAdData()).thenReturn(Optional.of(monetizableLeaveBehindAd));

        listener.onClickThrough(activity);

        assertThat(monetizableLeaveBehindAd.isMetaAdClicked()).isTrue();
    }

    @Test
    public void onClickthroughForUserProfileDeeplinkShouldCloserPlayer() {
        final AudioAd userAudioAd = AdFixtures.getAudioAdWithCustomClickthrough("soundcloud://users/42", Urn.forTrack(123L));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(userAudioAd));

        listener.onClickThrough(activity());

        PlayerUICommand event = eventBus.firstEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isAutomaticCollapse()).isTrue();
    }

    @Test
    public void onClickthroughForPlaylistDeeplinkShouldClosePlayer() {
        final AudioAd playlistAudioAd = AdFixtures.getAudioAdWithCustomClickthrough("soundcloud://playlists/42", Urn.forTrack(123L));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(playlistAudioAd));

        listener.onClickThrough(activity());

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isAutomaticCollapse()).isTrue();
    }

    @Test
    public void onClickthroughforUserProfileDeeplinkshouldStartProfileActivityAfterPlayerUICollapsed() {
        AppCompatActivity activity = activity();
        final AudioAd userAudioAd = AdFixtures.getAudioAdWithCustomClickthrough("soundcloud://users/42", Urn.forTrack(123L));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(userAudioAd));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createAudioAd(userAudioAd));

        listener.onClickThrough(activity);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(playQueueManager).moveToNextPlayableItem();
        verify(navigator).navigateTo(activity, NavigationTarget.forProfile(Urn.forUser(42L)));
    }

    @Test
    public void onClickthroughforPlaylistDeeplinkshouldStartPlaylistActivityAfterPlayerUICollapsed() {
        final AudioAd playlistAudioAd = AdFixtures.getAudioAdWithCustomClickthrough("soundcloud://playlists/42", Urn.forTrack(123L));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(playlistAudioAd));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createAudioAd(playlistAudioAd));
        when(playQueueManager.getScreenTag()).thenReturn("stream:main");

        AppCompatActivity activity = activity();
        listener.onClickThrough(activity);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(playQueueManager).moveToNextPlayableItem();
        verify(navigator).navigateTo(activity, NavigationTarget.forLegacyPlaylist(Urn.forPlaylist(42L), Screen.STREAM));
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
        Activity activity = activity();
        listener.onAboutAds(activity);
        verify(whyAdsPresenter).show(activity);
    }

}
