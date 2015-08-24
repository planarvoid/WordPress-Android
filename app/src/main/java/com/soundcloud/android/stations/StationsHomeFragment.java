package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class StationsHomeFragment extends LightCycleSupportFragment implements RefreshableScreen {
    @Inject @LightCycle StationsHomePresenter stationsHomePresenter;

    public StationsHomeFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.stations_fragment, container, false);
    }


    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.stations_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{stationsHomePresenter.getRecyclerView(), stationsHomePresenter.getEmptyView()};    }
}
