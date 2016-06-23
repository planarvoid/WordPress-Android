package com.soundcloud.android.profile;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import javax.inject.Inject;

public class UserPlaylistsFragment extends LightCycleSupportFragment<UserPlaylistsFragment>
        implements RefreshableScreen {

    @Inject @LightCycle UserPlaylistsPresenter presenter;

    public static UserPlaylistsFragment create(Urn userUrn,
                                               Screen screen,
                                               SearchQuerySourceInfo searchQuerySourceInfo) {
        UserPlaylistsFragment fragment = new UserPlaylistsFragment();
        fragment.setArguments(ProfileArguments.from(userUrn, screen, searchQuerySourceInfo));
        return fragment;
    }

    public UserPlaylistsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
