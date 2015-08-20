package com.soundcloud.android.stations;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class RecentStationsPresenter extends RecyclerViewPresenter<Station> {
    private final StationsOperations operations;
    private final RecentStationsAdapter adapter;
    private final Resources resources;
    @InjectView(R.id.ak_recycler_view) RecyclerView recyclerView;

    @Inject
    public RecentStationsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                   StationsOperations operations,
                                   RecentStationsAdapter adapter,
                                   Resources resources) {
        super(swipeRefreshAttacher, Options.cards());
        this.operations = operations;
        this.adapter = adapter;
        this.resources = resources;
    }

    @Override
    protected CollectionBinding<Station> onBuildBinding(Bundle bundle) {
        return CollectionBinding.from(operations.recentStations())
                .withAdapter(adapter)
                .build();
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.inject(this, view);
        // TODO : Replace this with layout attributes when AK allows it.
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(), resources.getInteger(R.integer.stations_grid_span_count)));
    }

    @Override
    protected void onItemClicked(View view, int i) {

    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
