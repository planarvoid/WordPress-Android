package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class RemoteControlReceiverTest extends AndroidUnitTest {
    private RemoteControlReceiver receiver;

    @Mock private Context context;
    @Mock private DateProvider dateProvider;
    @Captor private ArgumentCaptor<Intent> captor;

    @Before
    public void setUp() throws Exception {
        receiver = new RemoteControlReceiver(dateProvider);
        receiver.resetLastClicked();
    }

    @Test
    public void shouldSendPlaybackToggleActionBroadcastOnReceivingMediaPlayPauseEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        verifyBroadcastedAction(PlaybackAction.TOGGLE_PLAYBACK);
    }

    @Test
    public void shouldPutExtraEventSourceWhenMediaPlayPauseEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        verifySourceRemoteBroadcasted();
    }

    @Test
    public void shouldSendPlaybackToggleActionBroadcastOnReceivingHeadSetHookEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_HEADSETHOOK);
        verifyBroadcastedAction(PlaybackAction.TOGGLE_PLAYBACK);
    }

    @Test
    public void shouldSendNextAndPlayActionBroadcastOnReceivingHeadSetHookDoubleClickEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_HEADSETHOOK);
        when(dateProvider.getCurrentTime()).thenReturn(300L);

        receiveMediaIntent(KeyEvent.KEYCODE_HEADSETHOOK);
        verifyBroadcastedActions(PlaybackAction.TOGGLE_PLAYBACK,
                PlaybackAction.NEXT,
                PlaybackAction.PLAY);
    }

    @Test
    public void shouldNotSendNextActionBroadcastOnReceivingHeadSetHookDoubleClickTooLongEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_HEADSETHOOK);

        when(dateProvider.getCurrentTime()).thenReturn(1000L);
        receiveMediaIntent(KeyEvent.KEYCODE_HEADSETHOOK);
        verifyBroadcastedActions(PlaybackAction.TOGGLE_PLAYBACK,
                PlaybackAction.TOGGLE_PLAYBACK);
    }


    @Test
    public void shouldPutExtraEventSourceWhenMediaHeadSetHookEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_HEADSETHOOK);
        verifySourceRemoteBroadcasted();
    }

    @Test
    public void shouldSendPlaybackPauseEventToThePlaybackServiceOnReceivingMediaPauseEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PAUSE);
        verifyBroadcastedAction(PlaybackAction.PAUSE);
    }

    @Test
    public void shouldPutExtraEventSourceWhenMediaPauseEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PAUSE);
        verifySourceRemoteBroadcasted();
    }

    @Test
    public void shouldSendPlaybackPlayEventToThePlaybackServiceOnReceivingMediaPlayEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY);
        verifyBroadcastedAction(PlaybackAction.PLAY);
    }

    @Test
    public void shouldPutExtraEventSourceWhenMediaPlayEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY);
        verifySourceRemoteBroadcasted();
    }

    @Test
    public void shouldSendPlaybackPreviousActionBroadcastOnReceivingMediaPreviousEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        verifyBroadcastedAction(PlaybackAction.PREVIOUS);
    }

    @Test
    public void shouldPutExtraEventSourceWhenMediaPreviousEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        verifySourceRemoteBroadcasted();
    }

    @Test
    public void shouldSendPlaybackNextActionBroadcastOnReceivingMediaNextEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_NEXT);
        verifyBroadcastedAction(PlaybackAction.NEXT);
    }

    @Test
    public void shouldPutExtraEventSourceWhenMediaNextEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_NEXT);
        verifySourceRemoteBroadcasted();
    }

    @Test
    public void shouldIgnoreMediaRewindEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_REWIND);
        verifyNoMoreInteractions(context);
    }

    private void receiveMediaIntent(final int keycode) {
        receiver.onReceive(context, createMediaIntentForAction(keycode));
    }

    private Intent createMediaIntentForAction(final int keycode) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keycode);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        return intent;
    }

    private void verifyBroadcastedAction(final String playbackAction) {
        verify(context).sendBroadcast(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(playbackAction);
    }

    private void verifyBroadcastedActions(String... playbackActions) {
        int index = 0;

        verify(context, atLeast(playbackActions.length)).sendBroadcast(captor.capture());

        for (String action : playbackActions) {
            assertThat(captor.getAllValues().get(index++).getAction()).isEqualTo(action);
        }
    }

    private void verifySourceRemoteBroadcasted() {
        verify(context).sendBroadcast(captor.capture());
        expectExtraEqualsRemoteSource();
    }

    private void expectExtraEqualsRemoteSource() {
        assertThat(captor.getValue().getStringExtra(PlayControlEvent.EXTRA_EVENT_SOURCE))
                .isEqualTo(PlayControlEvent.SOURCE_REMOTE);
    }
}
