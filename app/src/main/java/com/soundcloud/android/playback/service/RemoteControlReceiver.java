package com.soundcloud.android.playback.service;

import static com.soundcloud.android.playback.service.PlaybackService.Actions;

import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.external.PlaybackAction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        sendPlaybackAction(context, PlaybackAction.TOGGLE_PLAYBACK);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        sendLegacyPlaybackAction(context, Actions.PAUSE_ACTION);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        sendLegacyPlaybackAction(context, Actions.PLAY_ACTION);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        sendPlaybackAction(context, PlaybackAction.PREVIOUS);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        sendPlaybackAction(context, PlaybackAction.NEXT);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        break; // No-op
                }
            }
        }
    }

    @Deprecated
    private void sendLegacyPlaybackAction(Context context, String action) {
        context.startService(createIntentForAction(action));
    }

    private void sendPlaybackAction(Context context, String action) {
        context.sendBroadcast(createIntentForAction(action));
    }

    private Intent createIntentForAction(String action) {
        return new Intent(action).putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_REMOTE);
    }
}
