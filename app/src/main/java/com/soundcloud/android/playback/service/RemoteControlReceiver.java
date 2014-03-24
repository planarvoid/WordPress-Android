package com.soundcloud.android.playback.service;

import static com.soundcloud.android.playback.service.PlaybackService.Actions;

import com.soundcloud.android.events.PlayControlEvent;

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
                        sendPlaybackAction(context, Actions.TOGGLEPLAYBACK_ACTION);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        sendPlaybackAction(context, Actions.PAUSE_ACTION);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        sendPlaybackAction(context, Actions.PLAY_ACTION);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        sendPlaybackAction(context, Actions.PREVIOUS_ACTION);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        sendPlaybackAction(context, Actions.NEXT_ACTION);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        break; // No-op
                }
            }
        }
    }

    private void sendPlaybackAction(Context context, String action) {
        context.startService(new Intent(action).putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_REMOTE));
    }
}
