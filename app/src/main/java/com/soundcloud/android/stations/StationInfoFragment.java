package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class StationInfoFragment extends LightCycleSupportFragment<StationInfoFragment> implements RefreshableScreen {
    static final String EXTRA_URN = "urn";
    static final String EXTRA_SOURCE = "source";
    static final String EXTRA_SEED_TRACK = "seed_track";

    @Inject LeakCanaryWrapper leakCanaryWrapper;
    @Inject @LightCycle StationInfoPresenter stationInfoPresenter;

    public static Fragment create(Urn stationUrn, Urn seedTrack, String source) {
        final Fragment fragment = new StationInfoFragment();
        final Bundle bundle = new Bundle();
        Urns.writeToBundle(bundle, EXTRA_URN, stationUrn);
        bundle.putString(EXTRA_SOURCE, source);
        Urns.writeToBundle(bundle, EXTRA_SEED_TRACK, seedTrack);
        fragment.setArguments(bundle);

        return fragment;
    }

    public StationInfoFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
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
        return new View[]{stationInfoPresenter.getRecyclerView(), stationInfoPresenter.getEmptyView()};
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }
}
