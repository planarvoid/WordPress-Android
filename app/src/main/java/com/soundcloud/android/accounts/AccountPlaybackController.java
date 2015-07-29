package com.soundcloud.android.accounts;

import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class AccountPlaybackController extends DefaultActivityLightCycle<AppCompatActivity> {
    private final AccountOperations accountOperations;
    private final PlaybackOperations playbackOperations;

    @Inject
    public AccountPlaybackController(AccountOperations accountOperations, PlaybackOperations playbackOperations) {
        this.accountOperations = accountOperations;
        this.playbackOperations = playbackOperations;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        if (!accountOperations.isUserLoggedIn()) {
            playbackOperations.resetService();
            activity.finish();
        }
    }
}
