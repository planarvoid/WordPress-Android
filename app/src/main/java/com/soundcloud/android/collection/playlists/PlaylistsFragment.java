package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlaylistsFragment extends LightCycleSupportFragment<PlaylistsFragment>
        implements RefreshableScreen {

    @Inject LeakCanaryWrapper leakCanaryWrapper;
    @Inject @LightCycle PlaylistsPresenter presenter;

    public static PlaylistsFragment create(PlaylistsOptions.Entities entities) {
        final Bundle bundle = PlaylistsArguments.from(entities);

        PlaylistsFragment fragment = new PlaylistsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public PlaylistsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }
}
