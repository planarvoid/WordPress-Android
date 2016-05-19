package com.soundcloud.android.playback.mediasession;

import com.soundcloud.android.BuildConfig;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;

import javax.inject.Inject;

// Note: just an injectable wrapper for testing purposes
class MediaSessionWrapper {

    @Inject
    MediaSessionWrapper() {
        // dagger
    }

    MediaSessionCompat getMediaSession(Context context, String tag) {
        ComponentName receiver = new ComponentName(BuildConfig.APPLICATION_ID, MediaButtonReceiver.class.getName());
        return new MediaSessionCompat(context, tag, receiver, null);
    }

    AudioManager getAudioManager(Context context) {
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    void handleIntent(MediaSessionCompat mediaSession, Intent intent) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
    }

}
