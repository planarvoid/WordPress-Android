package com.soundcloud.android.accounts;

import com.soundcloud.android.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.android.playback.PlaybackOperations;

import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

public class AccountPlaybackControlLightCycle extends DefaultActivityLightCycle {
    private final AccountOperations accountOperations;
    private final PlaybackOperations playbackOperations;

    @Inject
    public AccountPlaybackControlLightCycle(AccountOperations accountOperations, PlaybackOperations playbackOperations) {
        this.accountOperations = accountOperations;
        this.playbackOperations = playbackOperations;
    }

    @Override
    public void onResume(FragmentActivity activity) {
        if (!accountOperations.isUserLoggedIn()) {
            playbackOperations.resetService();
            activity.finish();
        }
    }
}
