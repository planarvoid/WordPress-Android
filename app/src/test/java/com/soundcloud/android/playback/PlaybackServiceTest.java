package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.notification.PlaybackNotificationController;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.Intent;
import android.media.AudioManager;

public class PlaybackServiceTest extends AndroidUnitTest {

    private static final int DURATION = 1000;
    private static final long START_POSITION = 123L;
    private PlaybackService playbackService;
    private PlaybackItem playbackItem = AudioPlaybackItem.create(TestPropertySets.fromApiTrack(), START_POSITION);
    private Urn track = playbackItem.getUrn();
    private TestEventBus eventBus = new TestEventBus();
    private TestDateProvider dateProvider = new TestDateProvider();

    @Mock private ApplicationProperties applicationProperties;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AccountOperations accountOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private StreamPlayer streamPlayer;
    @Mock private PlaybackReceiver.Factory playbackReceiverFactory;
    @Mock private PlaybackReceiver playbackReceiver;
    @Mock private IRemoteAudioManager remoteAudioManager;
    @Mock private PlayQueue playQueue;
    @Mock private Player.StateTransition stateTransition;
    @Mock private PlaybackNotificationController playbackNotificationController;
    @Mock private PlaybackSessionAnalyticsController analyticsController;
    @Mock private AdsController adsController;
    @Mock private AdsOperations adsOperations;

    @Before
    public void setUp() throws Exception {
        playbackService = new PlaybackService(eventBus,
                accountOperations, streamPlayer,playbackReceiverFactory,
                InjectionSupport.lazyOf(remoteAudioManager), playbackNotificationController,
                analyticsController, adsOperations, adsController);

        when(playbackReceiverFactory.create(playbackService, accountOperations, eventBus)).thenReturn(playbackReceiver);
    }

    @Test
    public void onCreateSetsServiceAsListenerOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        verify(streamPlayer).setListener(playbackService);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForToggleplaybackAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.TOGGLE_PLAYBACK);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPauseAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.PAUSE);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForSeek() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.SEEK);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForResetAllAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.RESET_ALL);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForStopAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.STOP);
    }

    @Test
    public void onCreateRegistersNoisyListenerToListenForAudioBecomingNoisyBroadcast() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    }

    @Test
    public void onCreatePublishedServiceLifecycleForCreated() throws Exception {
        playbackService.onCreate();

        PlayerLifeCycleEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYER_LIFE_CYCLE);
        assertThat(broadcasted.getKind()).isEqualTo(PlayerLifeCycleEvent.STATE_CREATED);
    }

    @Test
    public void onDestroyPublishedServiceLifecycleForDestroyed() throws Exception {
        playbackService.onCreate();
        playbackService.onDestroy();

        PlayerLifeCycleEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYER_LIFE_CYCLE);
        assertThat(broadcasted.getKind()).isEqualTo(PlayerLifeCycleEvent.STATE_DESTROYED);
    }

    @Test
    public void onStartPublishesServiceLifecycleForStarted() throws Exception {
        playbackService.onCreate();
        playbackService.stop();
        playbackService.onStartCommand(new Intent(), 0, 0);

        PlayerLifeCycleEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYER_LIFE_CYCLE);
        assertThat(broadcasted.getKind()).isEqualTo(PlayerLifeCycleEvent.STATE_STARTED);
    }

    @Test
    public void onStartWithNullIntentStopsSelf() throws Exception {
        playbackService.onCreate();
        playbackService.onStartCommand(null, 0, 0);

        assertThat(playbackService).hasStoppedSelf();
    }

    @Test
    public void onStartWillSubscribePlaybackNotification() {
        playbackService.onStartCommand(null, 0, 0);

        verify(playbackNotificationController).subscribe(playbackService);
    }

    @Test
    public void onPlaystateChangedPublishesStateTransistionForVideo() throws Exception {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123));
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);
        playbackItem = VideoPlaybackItem.create(videoAd);
        playbackService.onCreate();
        playbackService.play(playbackItem);

        final Player.StateTransition stateTransition = new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, videoAd.getAdUrn(), 0, 123, dateProvider);
        playbackService.onPlaystateChanged(stateTransition);

        Player.StateTransition broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
        assertThat(broadcasted).isEqualTo(stateTransition);
    }

    @Test
    public void onPlaystateChangedPublishesStateTransition() throws Exception {
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);

        playbackService.onCreate();
        playbackService.play(playbackItem);

        final Player.StateTransition stateTransition = new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, track, 0, 123);
        playbackService.onPlaystateChanged(stateTransition);

        Player.StateTransition broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
        assertThat(broadcasted).isEqualTo(stateTransition);
    }

    @Test
    public void onPlaystateChangedPublishesStateTransitionWithCorrectedDuration() throws Exception {
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);

        playbackService.onCreate();
        playbackService.play(playbackItem);

        final Player.StateTransition stateTransition = new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, track, 0, 0, dateProvider);
        playbackService.onPlaystateChanged(stateTransition);

        Player.StateTransition broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
        assertThat(broadcasted).isEqualTo(new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, track, 0, playbackItem.getDuration(), dateProvider));
    }

    @Test
    public void onPlaystateChangedDoesNotPublishStateTransitionWithDifferentUrnThanCurrent() {
        when(stateTransition.getUrn()).thenReturn(track);

        playbackService.onPlaystateChanged(stateTransition);

        assertThat(eventBus.eventsOn(EventQueue.PLAYBACK_STATE_CHANGED)).isEmpty();
    }

    @Test
    public void shouldForwardPlayerStateTransitionToAdsController() {
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);

        playbackService.onCreate();
        playbackService.play(playbackItem);

        final Player.StateTransition stateTransition = new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, track, 0, 123);
        playbackService.onPlaystateChanged(stateTransition);

        verify(adsController).onPlayStateTransition(stateTransition);
    }

    @Test
    public void shouldForwardPlayerStateTransitionToAnalyticsController() {
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);

        playbackService.onCreate();
        playbackService.play(playbackItem);

        final Player.StateTransition stateTransition = new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, track, 0, 123);
        playbackService.onPlaystateChanged(stateTransition);

        verify(analyticsController).onStateTransition(stateTransition);
    }

    @Test
    public void doesNotForwardPlayerStateTransitionToAnalyticsControllerWithDifferentUrnThenCurrent() {
        when(stateTransition.getUrn()).thenReturn(track);

        playbackService.onPlaystateChanged(stateTransition);

        verify(analyticsController, never()).onStateTransition(any(Player.StateTransition.class));
    }

    @Test
    public void onProgressForwardsProgressEventToAnalyticsController() {
        playbackService.onCreate();
        playbackService.play(playbackItem);
        playbackService.onProgressEvent(25, 50);

        ArgumentCaptor<PlaybackProgressEvent> captor = ArgumentCaptor.forClass(PlaybackProgressEvent.class);
        verify(analyticsController).onProgressEvent(captor.capture());

        final PlaybackProgressEvent event = captor.getValue();
        assertThat(event.getPlaybackProgress().getPosition()).isEqualTo(25);
        assertThat(event.getPlaybackProgress().getDuration()).isEqualTo(50);
        assertThat(event.getUrn()).isEqualTo(playbackItem.getUrn());
    }

    @Test
    public void onProgressPublishesAProgressEventForTrack() throws Exception {
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);
        playbackService.onCreate();
        playbackService.play(playbackItem);

        playbackService.onProgressEvent(123L, 456L);

        PlaybackProgressEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS);
        assertThat(broadcasted.getUrn().isTrack()).isTrue();
        assertThat(broadcasted.getUrn()).isEqualTo(track);
        assertThat(broadcasted.getPlaybackProgress().getPosition()).isEqualTo(123L);
        assertThat(broadcasted.getPlaybackProgress().getDuration()).isEqualTo(456L);
    }

    @Test
    public void onProgressPublishesAProgressEventForVideo() throws Exception {
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);
        playbackItem = VideoPlaybackItem.create(AdFixtures.getVideoAd(Urn.forTrack(123)));
        playbackService.onCreate();
        playbackService.play(playbackItem);

        playbackService.onProgressEvent(123L, 456L);

        PlaybackProgressEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS);
        assertThat(broadcasted.getUrn().isAd()).isTrue();
        assertThat(broadcasted.getUrn()).isEqualTo(Urn.forAd("dfp", "905"));
        assertThat(broadcasted.getPlaybackProgress().getPosition()).isEqualTo(123L);
        assertThat(broadcasted.getPlaybackProgress().getDuration()).isEqualTo(456L);
    }

    @Test
    public void openCurrentWithStreamableTrackCallsPlayOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);

        playbackService.play(playbackItem);

        verify(streamPlayer).play(playbackItem);
    }

    @Test
    public void stopCallsStopOnStreamPlaya() throws Exception {
        playbackService.stop();
        verify(streamPlayer).stop();
    }

    @Test
    public void callingStopSuppressesIdleNotifications() throws Exception {
        playbackService.onCreate();

        when(streamPlayer.getLastStateTransition()).thenReturn(new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, track));
        playbackService.play(playbackItem);

        playbackService.stop();
        playbackService.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.NONE, track));

        assertThat(playbackService).doesNotHaveLastForegroundNotification();
    }
}
