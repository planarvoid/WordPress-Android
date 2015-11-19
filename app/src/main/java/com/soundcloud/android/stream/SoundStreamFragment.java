package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.ScrollContent;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SoundStreamFragment extends LightCycleSupportFragment implements RefreshableScreen, ScrollContent {

    @Inject @LightCycle SoundStreamPresenter presenter;
    @Inject FeatureFlags featureFlags;

    public SoundStreamFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResource(), container, false);
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{presenter.getRecyclerView(), presenter.getEmptyView()};
    }

    @Override
    public void resetScroll() {
        presenter.scrollToTop();
    }

    private int getLayoutResource() {
        return featureFlags.isEnabled(Flag.NEW_STREAM)
                ? R.layout.stream_recyclerview_with_refresh : R.layout.default_recyclerview_with_refresh;
    }
}
