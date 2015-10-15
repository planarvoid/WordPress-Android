package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserPostsFragment extends ScrollableProfileFragment {

    static final String USER_URN_KEY = "user_urn_key";
    static final String SCREEN_KEY = "screen_key";
    static final String SEARCH_QUERY_SOURCE_INFO_KEY = "search_query_source_info_key";

    @Inject @LightCycle UserPostsPresenter presenter;

    public static UserPostsFragment create(Urn userUrn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(USER_URN_KEY, userUrn);
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
        return inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
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