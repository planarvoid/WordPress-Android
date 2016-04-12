package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class MyPlaylistsFragment extends ScrollableProfileFragment implements RefreshableScreen {

    @Inject @LightCycle MyPlaylistsPresenter presenter;

    public static MyPlaylistsFragment create(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        Bundle bundle = createBundle(screen, searchQuerySourceInfo);

        MyPlaylistsFragment fragment = new MyPlaylistsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public MyPlaylistsFragment() {
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

    @VisibleForTesting
    static Bundle createBundle(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(ProfileArguments.SCREEN_KEY, screen);
        bundle.putParcelable(ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);
        return bundle;
    }
}
