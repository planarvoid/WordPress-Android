package com.soundcloud.android.main;

import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleBinder;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

/*
 * If this is attached as a LightCycle, it's also necessary to override
 * onBackPressed() in the host Activity. It may be handled as a player close.
 */
public class PlayerController extends ActivityLightCycleDispatcher<AppCompatActivity> {
    final @LightCycle SlidingPlayerController playerController;
    final @LightCycle AdPlayerController adPlayerController;

    @Inject
    public PlayerController(SlidingPlayerController playerController,
                            AdPlayerController adPlayerController) {
        this.playerController = playerController;
        this.adPlayerController = adPlayerController;
        LightCycleBinder.bind(this);
    }

    public boolean handleBackPressed() {
        return playerController.handleBackPressed();
    }
}
