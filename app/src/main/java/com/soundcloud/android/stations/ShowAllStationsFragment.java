package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class ShowAllStationsFragment extends LightCycleSupportFragment implements RefreshableScreen {

    @Inject @LightCycle ShowAllStationsPresenter presenter;

    public static ShowAllStationsFragment create(int collectionType) {
        final ShowAllStationsFragment fragment = new ShowAllStationsFragment();
        final Bundle bundle = new Bundle();

        bundle.putAll(ShowAllStationsPresenter.createBundle(collectionType));
        fragment.setArguments(bundle);
        return fragment;
    }

    public ShowAllStationsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.stations_list, container, false);
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.stations_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{presenter.getRecyclerView(), presenter.getEmptyView()};
    }
}
