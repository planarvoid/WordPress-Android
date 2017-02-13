package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlayerFragment extends LightCycleSupportFragment<PlayerFragment> {

    @Inject @LightCycle PlayerPresenter presenter;

    public PlayerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player_fragment, container, false);
    }

    public void onPlayerSlide(float slideOffset) {
        presenter.onPlayerSlide(slideOffset);
    }

    public boolean handleBackPressed() {
        return presenter.handleBackPressed();
    }

    @Nullable
    public PlayerTrackPager getPlayerPager() {
        View view = getView();
        return view != null ? (PlayerTrackPager) view.findViewById(R.id.player_track_pager) : null;
    }
}
