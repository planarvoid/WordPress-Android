package com.soundcloud.android.stations;

import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class RecentStationsPresenter extends RecyclerViewPresenter<Station> {
    private final StationsOperations operations;
    private final RecentStationsAdapter adapter;

    @Inject
    public RecentStationsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                   StationsOperations operations,
                                   RecentStationsAdapter adapter) {
        super(swipeRefreshAttacher);
        this.operations = operations;
        this.adapter = adapter;
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
    protected void onItemClicked(View view, int i) {

    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
