package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlayerFragment extends LightCycleSupportFragment {

    @Inject @LightCycle PlayerPresenter controller;

    public PlayerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player_fragment, container, false);
    }

    public void onPlayerSlide(float slideOffset) {
        controller.onPlayerSlide(slideOffset);
    }
}
