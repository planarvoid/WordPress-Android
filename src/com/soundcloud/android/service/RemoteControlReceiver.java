package com.soundcloud.android.service;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.ICloudPlaybackService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;

@SuppressWarnings({"UnusedDeclaration"})
public class RemoteControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ICloudPlaybackService svc = (ICloudPlaybackService) peekService(context,
                new Intent(context, CloudPlaybackService.class));

        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
                if (svc != null) {
                    try {
                        switch (event.getKeyCode()) {
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            case KeyEvent.KEYCODE_HEADSETHOOK:
                                handleToggle(context, svc);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                handlePreviousTrack(context, svc);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_NEXT:
                                handleNextTrack(context, svc);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_REWIND:
                                handleRewind(context, svc);
                                break;
                        }
                    } catch (RemoteException ignored) {
                        Log.w(TAG, ignored);
                    }
                }
            }
        } else if ("android.media.AUDIO_BECOMING_NOISY".equals(intent.getAction())) {
            if (svc != null) {
                try {
                    if (svc.isPlaying()) svc.pause();
                } catch (RemoteException ignored) {
                }
            }
        }
    }

    private void handleRewind(Context context, ICloudPlaybackService svc) throws RemoteException {
        svc.restart();
    }

    private void handlePause(Context context, ICloudPlaybackService svc) throws RemoteException {
        svc.pause();
    }

    private void handleToggle(Context context, ICloudPlaybackService svc) throws RemoteException {
        svc.toggle();
    }

    private void handleNextTrack(Context context, ICloudPlaybackService svc) throws RemoteException {
        svc.next();
    }

    private void handlePreviousTrack(Context context, ICloudPlaybackService svc) throws RemoteException {
        svc.prev();
    }
}
