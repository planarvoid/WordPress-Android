package com.soundcloud.android.likes;

import com.soundcloud.android.Actions;

import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrackLikesIntentResolver {

    public static final String EXTRA_SOURCE = "EXTRA_SOURCE";

    private boolean shouldStartPlayback;

    @Inject
    TrackLikesIntentResolver() {
    }

    void onIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        if (isPlaybackIntent(intent)) {
            shouldStartPlayback = true;
        }
    }

    private boolean isPlaybackIntent(Intent intent) {
        return intent.hasExtra(EXTRA_SOURCE) && ((Intent) intent.getParcelableExtra(EXTRA_SOURCE)).getAction().equals(Actions.SHORTCUT_PLAY_LIKES);
    }

    boolean consumePlaybackRequest() {
        boolean play = shouldStartPlayback;
        if (shouldStartPlayback) {
            shouldStartPlayback = false;
        }

        return play;
    }
}
