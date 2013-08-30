package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.CloudPlaybackService.Actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        handleToggle(context);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        handlePause(context);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        handlePlay(context);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        handlePreviousTrack(context);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        handleNextTrack(context);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        handleRewind(context);
                        break;
                }
            }
        }
    }

    @SuppressWarnings("UnusedParameters")
    private void handleRewind(Context context) {
    }

    private void handleToggle(Context context) {
        context.startService(new Intent(Actions.TOGGLEPLAYBACK_ACTION));
    }

    private void handlePause(Context context) {
        context.startService(new Intent(Actions.PAUSE_ACTION));
    }

    private void handlePlay(Context context) {
        context.startService(new Intent(Actions.PLAY_ACTION));
    }

    private void handleNextTrack(Context context) {
        context.startService(new Intent(Actions.NEXT_ACTION));
    }

    private void handlePreviousTrack(Context context)  {
        context.startService(new Intent(Actions.PREVIOUS_ACTION));
    }
}
