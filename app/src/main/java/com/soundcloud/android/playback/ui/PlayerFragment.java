package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.utils.LightCycleLogger;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import com.soundcloud.lightcycle.SupportFragmentLightCycle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlayerFragment extends LightCycleSupportFragment<PlayerFragment> {

    @Inject @LightCycle PlayerPresenter presenter;
    // investigation of https://soundcloud.atlassian.net/browse/DROID-1781
    @LightCycle SupportFragmentLightCycle<Fragment> logger = LightCycleLogger.forSupportFragment("PlayerFragment");
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    public PlayerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.player_fragment, container, false);

        final int elevation = view.getResources().getDimensionPixelSize(R.dimen.player_elevation);
        ViewCompat.setElevation(view, elevation);

        return view;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }
}
