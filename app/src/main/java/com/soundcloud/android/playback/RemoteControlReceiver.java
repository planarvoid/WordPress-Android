package com.soundcloud.android.playback;

import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.external.PlaybackAction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.view.KeyEvent;

@edu.umd.cs.findbugs.annotations.SuppressWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "As agreed on this specific case, we are ok with this, since it will be a big win for a small hack.")
public class RemoteControlReceiver extends BroadcastReceiver {
    private static final int DOUBLE_CLICK_DELAY = 400;
    private static long lastClicked = -DOUBLE_CLICK_DELAY;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        long time = SystemClock.uptimeMillis();

                        if(time - lastClicked < DOUBLE_CLICK_DELAY) {
                            sendPlaybackAction(context, PlaybackAction.NEXT);
                            sendPlaybackAction(context, PlaybackAction.PLAY);
                        } else {
                            sendPlaybackAction(context, PlaybackAction.TOGGLE_PLAYBACK);
                        }

                        lastClicked = time;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        sendPlaybackAction(context, PlaybackAction.PAUSE);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        sendPlaybackAction(context, PlaybackAction.PLAY);
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

    private void sendPlaybackAction(Context context, String action) {
        context.sendBroadcast(createIntentForAction(action));
    }

    private Intent createIntentForAction(String action) {
        return new Intent(action).putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_REMOTE);
    }
    @VisibleForTesting
    public void resetLastClicked() {
        lastClicked = -DOUBLE_CLICK_DELAY;
    }
}
