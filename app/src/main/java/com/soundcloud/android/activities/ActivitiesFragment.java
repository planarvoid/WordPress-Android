package com.soundcloud.android.activities;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class ActivitiesFragment extends LightCycleSupportFragment<ActivitiesFragment> implements RefreshableScreen {

    @Inject @LightCycle ActivitiesPresenter presenter;

    public ActivitiesFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResource(), container, false);
    }

    private int getLayoutResource() {
        return R.layout.default_recyclerview_with_refresh_and_new_items_indicator;
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{presenter.getRecyclerView(), presenter.getEmptyView()};
    }
}
