package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class ChartFragment extends LightCycleSupportFragment<ChartFragment> implements RefreshableScreen {

    public static final String EXTRA_TYPE = "chartType";
    public static final String EXTRA_GENRE_URN = "chartGenreUrn";

    @Inject @LightCycle ChartPresenter presenter;

    public static ChartFragment create(ChartType type, Urn genreUrn) {
        final Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_TYPE, type);
        bundle.putParcelable(EXTRA_GENRE_URN, genreUrn);

        final ChartFragment fragment = new ChartFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public ChartFragment() {
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
