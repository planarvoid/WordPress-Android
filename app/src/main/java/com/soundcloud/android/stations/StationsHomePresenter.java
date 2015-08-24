package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import rx.Observable;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class StationsHomePresenter extends RecyclerViewPresenter<StationBucket> {
    private final Resources resources;
    private final StationsOperations operations;
    private final StationsHomeAdapter adapter;

    @Inject
    public StationsHomePresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                 Resources resources,
                                 StationsOperations operations,
                                 StationsHomeAdapter adapter) {
        super(swipeRefreshAttacher, Options.cards());
        this.resources = resources;
        this.operations = operations;
        this.adapter = adapter;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getRecyclerView().setHasFixedSize(true);
    }

    @Override
    protected CollectionBinding<StationBucket> onBuildBinding(Bundle bundle) {
        return CollectionBinding
                .from(buckets())
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<StationBucket> onRefreshBinding() {
        return CollectionBinding
                .from(buckets())
                .withAdapter(adapter)
                .build();
    }

    private Observable<List<StationBucket>> buckets() {
        // TODO : Load saved, ... and concat
        return Observable.concat(recent(), recent()).toList();
    }

    private Observable<StationBucket> recent() {
        return operations
                .recentStations()
                .take(maxNumberOfStations())
                .toList()
                .map(StationBucket.fromStations(resources.getString(R.string.recent_stations_title)));
    }

    private int maxNumberOfStations() {
        return resources.getInteger(R.integer.stations_grid_span_count) * resources.getInteger(R.integer.stations_home_bucket_max_row_count);
    }

    @Override
    protected void onItemClicked(View view, int i) {
        // no-op
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    static class StationsHomeAdapter extends RecyclerItemAdapter<StationBucket, StationsHomeAdapter.StationViewHolder> {

        @Inject
        public StationsHomeAdapter(StationsHomeRenderer renderer) {
            super(renderer);
        }

        @Override
        protected StationViewHolder createViewHolder(View view) {
            return new StationViewHolder(view);
        }

        @Override
        public int getBasicItemViewType(int i) {
            return 0;
        }

        public class StationViewHolder extends RecyclerView.ViewHolder {

            public StationViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
