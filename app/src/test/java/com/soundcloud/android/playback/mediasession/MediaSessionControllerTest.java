package com.soundcloud.android.playback.mediasession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioPlaybackItem;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.external.PlaybackActionController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;

import android.app.Notification;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class MediaSessionControllerTest extends AndroidUnitTest {

    private static final long START_POSITION = 2500L;
    private static final Track TRACK = ModelFixtures.track();
    private static final PlaybackItem PLAYBACK_ITEM = AudioPlaybackItem.create(TRACK, START_POSITION);
    private static final Urn URN = PLAYBACK_ITEM.getUrn();
    private static final PlayQueueItem PLAY_QUEUE_ITEM = TestPlayQueueItem.createTrack(URN);
    private static final PlayQueueItem AD_QUEUE_ITEM = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(URN));


    @Mock MediaSessionController.Listener listener;
    @Mock MediaSessionWrapper mediaSessionWrapper;
    @Mock PlaybackActionController actionController;
    @Mock MetadataOperations metadataOperations;
    @Mock PlayQueueManager playQueueManager;
    @Mock AdsOperations adsOperations;

    @Mock AudioManager audioManager;
    @Mock MediaSessionCompat mediaSession;
    @Mock MediaControllerCompat mediaController;
    @Mock MediaMetadataCompat mediaMetadata;
    @Mock MediaDescriptionCompat mediaDescription;

    @Mock MediaMetadataCompat currentMediaMetadata;
    @Mock MediaDescriptionCompat currentMediaDescription;
    @Mock Navigator navigator;
    private TestEventBus eventBus = new TestEventBus();

    private MediaSessionController controller;

    @Before
    public void setUp() {
        when(mediaSessionWrapper.getMediaSession(eq(context()), anyString())).thenReturn(mediaSession);
        when(mediaSessionWrapper.getAudioManager(context())).thenReturn(audioManager);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM);

        controller = new MediaSessionController(context(), listener, mediaSessionWrapper,
                                                actionController, metadataOperations, playQueueManager, adsOperations,
                                                navigator, eventBus);

        setupMetadataMocks();
    }

    @Test
    public void onStartCommandHandlesIntent() {
        Intent intent = mock(Intent.class);

        controller.onStartCommand(intent);

        verify(mediaSessionWrapper).handleIntent(mediaSession, intent);
    }

    @Test
    public void onPlayRequestsAudioFocus() {
        setupWithAudioFocus(true);

        boolean focusGained = controller.onPlay();

        assertThat(focusGained).isTrue();
    }

    @Test
    public void onPlaySetsMediaSessionActive() {
        setupWithAudioFocus(true);

        controller.onPlay();

        verify(mediaSession).setActive(true);
    }

    @Test
    public void onPlayNotifiesFocusGain() {
        setupWithAudioFocus(true);

        controller.onPlay();

        verify(listener).onFocusGain();
    }

    @Test
    public void onPlayShowsNotification() {
        setupWithAudioFocus(true);

        controller.onPlay();

        verify(listener).showNotification(any(Notification.class));
    }

    @Test
    public void onPlayWithItemRequestsAudioFocus() {
        setupWithAudioFocus(true);

        boolean focusGained = controller.onPlay(PLAYBACK_ITEM);

        assertThat(focusGained).isTrue();
    }

    @Test
    public void onPlayWithItemSetsMediaSessionActive() {
        setupWithAudioFocus(true);

        controller.onPlay(PLAYBACK_ITEM);

        verify(mediaSession).setActive(true);
    }

    @Test
    public void onPlayWithItemNotifiesFocusGain() {
        setupWithAudioFocus(true);

        controller.onPlay(PLAYBACK_ITEM);

        verify(listener).onFocusGain();
    }

    @Test
    public void onPlayWithItemShowsNotification() {
        setupWithAudioFocus(true);

        controller.onPlay(PLAYBACK_ITEM);

        verify(listener).showNotification(any(Notification.class));
    }

    @Test
    public void onPlayWithItemBuildsAndShowsNewMetadata() {
        setupWithAudioFocus(true);

        controller.onPlay(PLAYBACK_ITEM);

        verify(metadataOperations).metadata(URN, false, Optional.of(currentMediaMetadata));
        verify(listener).showNotification(any(Notification.class));
    }

    @Test
    public void onPauseShowsNotification() {
        controller.onPause();

        verify(listener).showNotification(any(Notification.class));
    }

    @Test
    public void onPauseSetsPausedState() {
        controller.onPause();

        verifyStateAndPosition(PlaybackStateCompat.STATE_PAUSED, 0L);
    }

    @Test
    public void onStopAbandonsAudioFocus() {
        controller.onStop();

        verify(audioManager).abandonAudioFocus(any(AudioManager.OnAudioFocusChangeListener.class));
    }

    @Test
    public void onStopSetsMediaSessionAsInactive() {
        controller.onStop();

        verify(mediaSession).setActive(false);
    }

    @Test
    public void onSkipSendsTrackingEvent() {
        controller.onSkip();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.fromSystemSkip().getKind());
    }

    @Test
    public void onSkipUpdatesMetadata() {
        controller.onSkip();

        verify(listener).showNotification(any(Notification.class));
    }

    @Test
    public void onSkipDoesNotUpdateMetadataWhenEmpty() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);

        controller.onSkip();

        verify(listener, never()).showNotification(any(Notification.class));
    }

    @Test
    public void shouldRequestsMediaDataForAd() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AD_QUEUE_ITEM);

        controller.onSkip();

        verify(metadataOperations).metadata(Urn.forAd("dfp", "869"), true, Optional.of(currentMediaMetadata));
    }


    @Test
    public void onPreloadBuildsMetadata() {
        controller.onPreload(URN);

        verify(metadataOperations).preload(URN);
    }

    @Test
    public void onBufferingSetsState() {
        controller.onBuffering(START_POSITION);

        verifyStateAndPosition(PlaybackStateCompat.STATE_BUFFERING, START_POSITION);
    }

    @Test
    public void onPlayingSetsState() {
        controller.onPlaying(START_POSITION);

        verifyStateAndPosition(PlaybackStateCompat.STATE_PLAYING, START_POSITION);
    }

    @Test
    public void onSeekSetsState() {
        controller.onSeek(START_POSITION);

        verifyStateAndPosition(PlaybackStateCompat.STATE_NONE, START_POSITION);
    }

    @Test
    public void onProgressSetsState() {
        controller.onProgress(START_POSITION);

        verifyStateAndPosition(PlaybackStateCompat.STATE_NONE, START_POSITION);
    }

    @Test
    public void onDestroyReleasesSession() {
        controller.onDestroy();

        verify(mediaSession).release();
    }

    @Test
    public void testIsPlayingVideoAd() {
        when(adsOperations.isCurrentItemVideoAd()).thenReturn(true);

        assertThat(controller.isPlayingVideoAd()).isTrue();
    }

    private void setupMetadataMocks() {
        when(mediaSession.getController()).thenReturn(mediaController);
        when(mediaController.getMetadata()).thenReturn(currentMediaMetadata);
        when(currentMediaMetadata.getDescription()).thenReturn(currentMediaDescription);

        when(metadataOperations.metadata(any(Urn.class), anyBoolean(), eq(Optional.of(currentMediaMetadata))))
                .thenReturn(Observable.just(mediaMetadata));
        when(mediaMetadata.getDescription()).thenReturn(mediaDescription);
    }

    private void setupWithAudioFocus(boolean success) {
        int status = success
                     ? AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                     : AudioManager.AUDIOFOCUS_REQUEST_FAILED;

        when(audioManager.requestAudioFocus(
                any(AudioManager.OnAudioFocusChangeListener.class),
                eq(AudioManager.STREAM_MUSIC),
                eq(AudioManager.AUDIOFOCUS_GAIN))).thenReturn(status);

    }

    private void verifyStateAndPosition(int playbackState, long position) {
        ArgumentCaptor<PlaybackStateCompat> playbackStateCaptor = ArgumentCaptor.forClass(PlaybackStateCompat.class);

        // setPlaybackState is always called once on instance creation
        verify(mediaSession, atLeast(2)).setPlaybackState(playbackStateCaptor.capture());

        // get next event
        PlaybackStateCompat state = playbackStateCaptor.getAllValues().get(1);
        assertThat(state.getState()).isEqualTo(playbackState);
        assertThat(state.getPosition()).isEqualTo(position);
    }

}
