package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlayerFragment extends Fragment {

    @Inject PlayerPagerController controller;

    public PlayerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        controller.onViewCreated(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        controller.onResume();
    }

    @Override
    public void onPause() {
        controller.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        controller.onDestroyView();
        super.onDestroyView();
    }

    public void onPlayerSlide(float slideOffset) {
        controller.onPlayerSlide(slideOffset);
    }
}
