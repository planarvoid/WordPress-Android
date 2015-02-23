package com.soundcloud.android.accounts;

import com.soundcloud.android.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.playback.PlaybackOperations;

import android.support.v7.app.ActionBarActivity;

import javax.inject.Inject;

public class AccountPlaybackController extends DefaultLightCycleActivity<ActionBarActivity> {
    private final AccountOperations accountOperations;
    private final PlaybackOperations playbackOperations;

    @Inject
    public AccountPlaybackController(AccountOperations accountOperations, PlaybackOperations playbackOperations) {
        this.accountOperations = accountOperations;
        this.playbackOperations = playbackOperations;
    }

    @Override
    public void onResume(ActionBarActivity activity) {
        if (!accountOperations.isUserLoggedIn()) {
            playbackOperations.resetService();
            activity.finish();
        }
    }
}
