package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.MainPagerAdapter;
import com.soundcloud.android.main.ScrollContent;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class StreamFragment extends LightCycleSupportFragment<StreamFragment>
        implements RefreshableScreen, ScrollContent, MainPagerAdapter.FocusListener {

    @Inject @LightCycle StreamPresenter presenter;

    public StreamFragment() {
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
        return R.layout.recyclerview_with_refresh_and_page_bg;
    }

    @Override
    public void onFocusChange(boolean hasFocus) {
        presenter.onFocusChange(hasFocus);
    }
}
