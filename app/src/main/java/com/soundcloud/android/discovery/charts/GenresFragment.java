package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class GenresFragment extends LightCycleSupportFragment<ChartTracksFragment> {
    public static final String EXTRA_CHART_CATEGORY = "chartCategory";

    @Inject @LightCycle GenresPresenter presenter;

    static GenresFragment create(ChartCategory chartCategory) {
        final Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_CHART_CATEGORY, chartCategory);
        GenresFragment genresFragment = new GenresFragment();
        genresFragment.setArguments(bundle);
        return genresFragment;
    }

    public GenresFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.discovery_recycler_view, container, false);
    }
}
