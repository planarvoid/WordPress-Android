package com.soundcloud.android.service;

import static com.soundcloud.android.SoundCloudApplication.TAG;

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
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event != null &&
                event.getAction() == KeyEvent.ACTION_DOWN &&
                event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {

                ICloudPlaybackService svc =
                        (ICloudPlaybackService) peekService(context,
                                new Intent(context, CloudPlaybackService.class));

                if (svc != null) {
                    try {
                        if (svc.isPlaying()) {
                            svc.pause();
                        } else {
                            svc.play();
                        }
                    } catch (RemoteException e) {
                        Log.w(TAG, e);
                    }
                }
            }
        }
    }
}
