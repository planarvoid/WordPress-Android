package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserFollowingsFragment extends ScrollableProfileFragment {
    public static final String IS_CURRENT_USER = "is_current_user";

    @Inject LeakCanaryWrapper leakCanaryWrapper;
    @Inject @LightCycle UserFollowingsPresenter presenter;

    public static UserFollowingsFragment create(Urn userUrn,
                                                Screen screen,
                                                SearchQuerySourceInfo searchQuerySourceInfo) {
        UserFollowingsFragment fragment = new UserFollowingsFragment();
        fragment.setArguments(ProfileArguments.from(userUrn, screen, searchQuerySourceInfo));
        return fragment;
    }

    public static UserFollowingsFragment createForCurrentUser(Urn userUrn, Screen trackingScreen, SearchQuerySourceInfo searchQuerySourceInfo) {
        UserFollowingsFragment fragment = new UserFollowingsFragment();
        Bundle bundle = ProfileArguments.from(userUrn, trackingScreen, searchQuerySourceInfo);
        bundle.putBoolean(IS_CURRENT_USER, true);
        fragment.setArguments(bundle);
        return fragment;
    }

    public UserFollowingsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        leakCanaryWrapper.watch(this);
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
