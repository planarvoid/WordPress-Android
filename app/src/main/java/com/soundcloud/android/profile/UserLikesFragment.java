package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserLikesFragment extends LightCycleSupportFragment implements RefreshAware, ScrollableProfileItem {

    @Inject @LightCycle UserLikesPresenter presenter;

    public static UserLikesFragment create(Urn userUrn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        UserLikesFragment fragment = new UserLikesFragment();
        fragment.setArguments(ProfileArguments.from(userUrn,screen, searchQuerySourceInfo));
        return fragment;
    }

    public UserLikesFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.new_profile_recycle_view, container, false);
    }

    @Override
    public void attachRefreshLayout(MultiSwipeRefreshLayout refreshLayout) {
        presenter.attachRefreshLayout(refreshLayout);
    }

    @Override
    public void detachRefreshLayout() {
        presenter.detachRefreshLayout();
    }

    @Override
    public void setScrollListener(Listener scrollListener) {
        presenter.setScrollListener(scrollListener);
    }

    @Override
    public void configureOffsets(int currentHeaderHeight, int maxHeaderHeight) {
        presenter.configureOffsets(currentHeaderHeight, maxHeaderHeight);
    }
}