package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.peripherals.PeripheralsOperations;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.track.TrackOperations;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import dagger.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.BroadcastReceiver;
import android.media.AudioManager;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Iterator;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackServiceTest {
    
    PlaybackService playbackService;

    @Mock
    private EventBus eventBus;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private TrackOperations trackOperations;
    @Mock
    private PeripheralsOperations peripheralsOperations;
    @Mock
    private PlaybackEventSource playbackEventSource;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private PlayerAppWidgetProvider appWidgetProvider;
    @Mock
    private StreamPlaya streamPlayer;
    @Mock
    private PlaybackReceiver.Factory playbackReceiverFactory;
    @Mock
    private PlaybackReceiver playbackReceiver;
    @Mock
    private Lazy<IRemoteAudioManager> audioManagerProvider;
    @Mock
    private IRemoteAudioManager remoteAudioManager;

    @Before
    public void setUp() throws Exception {
        playbackService = new PlaybackService(playQueueManager, eventBus, trackOperations, peripheralsOperations,
                playbackEventSource, accountOperations, imageOperations, appWidgetProvider, streamPlayer,
                playbackReceiverFactory, audioManagerProvider);
        when(playbackReceiverFactory.create(playbackService, accountOperations, playQueueManager, eventBus)).thenReturn(playbackReceiver);
        when(audioManagerProvider.get()).thenReturn(remoteAudioManager);
    }

    @Test
    public void onCreateSetsServiceAsListenerOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        verify(streamPlayer).setListener(playbackService);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPlayAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.PLAY_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForToggleplaybackAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPauseAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.PAUSE_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForNextAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.NEXT_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPreviousAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.PREVIOUS_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForResetAllAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.RESET_ALL)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForStopAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.STOP_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForReloadQueueAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.RELOAD_QUEUE)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPlayQueueChangedAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForRetryRelatedTracksAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.RETRY_RELATED_TRACKS)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersNoisyListenerToListenForAudioBecomingNoisyBroadcast() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)).toContain(playbackReceiver);
    }

    private ArrayList<BroadcastReceiver> getReceiversForAction(String action) {
        ArrayList<BroadcastReceiver> broadcastReceivers = new ArrayList<BroadcastReceiver>();
        for (ShadowApplication.Wrapper registeredReceiver : Robolectric.getShadowApplication().getRegisteredReceivers()) {
            if (registeredReceiver.context == playbackService){
                Iterator<String> actions = registeredReceiver.intentFilter.actionsIterator();
                while (actions.hasNext()) {
                    if (actions.next().equals(action)) {
                        broadcastReceivers.add(registeredReceiver.broadcastReceiver);
                    }
                }
            }

        }
        return broadcastReceivers;
    }
}
