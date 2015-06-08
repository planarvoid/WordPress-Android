package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserDetailsFragment extends LightCycleSupportFragment implements ScrollableProfileItem, RefreshAware {

    @Inject @LightCycle UserDetailsView userDetailsView;
    @LightCycle UserDetailsPresenter userDetailsPresenter;

    public static UserDetailsFragment create() {
        return new UserDetailsFragment();
    }

    public UserDetailsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        userDetailsPresenter = new UserDetailsPresenter(userDetailsView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_info_view, container, false);
    }

    @Override
    public void setScrollListener(Listener scrollListener) {
        // no-op
    }

    @Override
    public void configureOffsets(int currentHeaderHeight, int maxHeaderHeight) {
        userDetailsPresenter.setHeaderSize(currentHeaderHeight);
    }

    @Override
    public void attachRefreshLayout(MultiSwipeRefreshLayout refreshLayout) {
        userDetailsPresenter.attachRefreshLayout(refreshLayout);
    }

    @Override
    public void detachRefreshLayout() {
        userDetailsPresenter.detachRefreshLayout();
    }
}
