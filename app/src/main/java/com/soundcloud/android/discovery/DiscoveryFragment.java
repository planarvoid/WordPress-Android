package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.MainPagerAdapter;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class DiscoveryFragment extends LightCycleSupportFragment<DiscoveryFragment>
        implements RefreshableScreen, MainPagerAdapter.ScrollContent {

    @Inject @LightCycle DiscoveryPresenter presenter;

    public DiscoveryFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
        fragmentView.setBackgroundColor(getResources().getColor(R.color.page_background));
        adjustProgressViewPosition(fragmentView);

        return fragmentView;
    }

    private void adjustProgressViewPosition(View fragmentView) {
        // On this specific screen, it has to be positioned a bit down - under the search bar.
        final int endPtrSpinnerPx = getContext().getResources()
                                                .getDimensionPixelSize(R.dimen.search_ptr_layout_end_position);
        final SwipeRefreshLayout swipeToRefreshLayout = (SwipeRefreshLayout) fragmentView.findViewById(R.id.str_layout);
        swipeToRefreshLayout.setProgressViewEndTarget(false, endPtrSpinnerPx);
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{presenter.getRecyclerView()};
    }

    @Override
    public void resetScroll() {
        presenter.scrollToTop();
    }
}
