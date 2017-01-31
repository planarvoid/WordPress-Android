package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.playback.PlaySessionController;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

/**
 * Hack around the expected behavior since Google's Cast SDK does not call
 * the correct methods for skip-next/skip-prev.
 *
 * Instead, we override the seek icons and callbacks to mimic skipping tracks.
 *
 * See: https://plus.google.com/112460979421262523023/posts/SsRRaxDWgs8?sfc=true
 */
public class CastMediaIntentReceiver extends MediaIntentReceiver {

    @Inject PlaySessionController playSessionController;

    @Override
    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);
        super.onReceive(context, intent);
    }

    @Override
    protected void onReceiveActionForward(Session session, long l) {
        playSessionController.nextTrack();
    }

    @Override
    protected void onReceiveActionRewind(Session session, long l) {
        playSessionController.previousTrack();
    }
}
