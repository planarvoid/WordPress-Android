package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;

@RunWith(SoundCloudTestRunner.class)
public class RemoteControlReceiverTest {

    RemoteControlReceiver receiver;

    @Mock
    Context context;

    @Captor
    ArgumentCaptor<Intent> captor;

    @Before
    public void setUp() throws Exception {
        receiver = new RemoteControlReceiver();
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
    public void shouldPutExtraEventSourceWhenMediaHeadSetHookEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_HEADSETHOOK);

        verifySourceRemoteBroadcasted();
    }

    @Test
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void shouldSendPlaybackPauseEventToThePlaybackServiceOnReceivingMediaPauseEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PAUSE);

        verifyActionSentToService(PlaybackService.Actions.PAUSE_ACTION);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void shouldPutExtraEventSourceWhenMediaPauseEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PAUSE);

        verifySourceRemoteSentToService();
    }

    @Test
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void shouldSendPlaybackPlayEventToThePlaybackServiceOnReceivingMediaPlayEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY);

        verifyActionSentToService(PlaybackService.Actions.PLAY_ACTION);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void shouldPutExtraEventSourceWhenMediaPlayEvent() throws Exception {
        receiveMediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY);

        verifySourceRemoteSentToService();
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
        expect(captor.getValue().getAction()).toEqual(playbackAction);
    }

    private void verifySourceRemoteBroadcasted() {
        verify(context).sendBroadcast(captor.capture());
        expectExtraEqualsRemoteSource();
    }

    private void verifyActionSentToService(final String playbackAction) {
        verify(context).startService(captor.capture());
        expect(captor.getValue().getAction()).toEqual(playbackAction);
    }

    private void verifySourceRemoteSentToService() {
        verify(context).startService(captor.capture());
        expectExtraEqualsRemoteSource();
    }

    private void expectExtraEqualsRemoteSource() {
        expect(captor.getValue().getStringExtra(PlayControlEvent.EXTRA_EVENT_SOURCE)).toEqual(PlayControlEvent.SOURCE_REMOTE);
    }
}