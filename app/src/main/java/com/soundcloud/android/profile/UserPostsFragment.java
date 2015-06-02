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

public class UserPostsFragment extends LightCycleSupportFragment implements RefreshAware, ScrollableProfileItem {

    static final String USER_URN_KEY = "user_urn_key";
    static final String USER_NAME_KEY = "user_name_key";
    static final String SCREEN_KEY = "screen_key";
    static final String SEARCH_QUERY_SOURCE_INFO_KEY = "search_query_source_info_key";

    @Inject @LightCycle UserPostsPresenter presenter;

    public static UserPostsFragment create(Urn userUrn, String username, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(USER_URN_KEY, userUrn);
        bundle.putString(USER_NAME_KEY, username);
        bundle.putSerializable(SCREEN_KEY, screen);
        bundle.putParcelable(SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);

        UserPostsFragment fragment = new UserPostsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public UserPostsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_recycle_view, container, false);
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