package com.soundcloud.android.discovery.systemplaylist;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import io.reactivex.Observable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SystemPlaylistFragment extends LightCycleSupportFragment<SystemPlaylistFragment> implements SystemPlaylistPresenter.SystemPlaylistView, RefreshableScreen {

    public static final String TAG = "SystemPlaylistFragment";
    static final String EXTRA_PLAYLIST_URN = "extra_urn";

    @Inject @LightCycle SystemPlaylistPresenter presenter;

    public static SystemPlaylistFragment newInstance(Urn urn) {
        final SystemPlaylistFragment fragment = new SystemPlaylistFragment();
        final Bundle args = new Bundle();
        Urns.writeToBundle(args, EXTRA_PLAYLIST_URN, urn);
        fragment.setArguments(args);
        return fragment;
    }

    public SystemPlaylistFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Observable<Long> onEnterScreenTimestamp() {
        return ((RootActivity) getActivity()).enterScreenTimestamp();
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
}
