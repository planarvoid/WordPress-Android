package com.soundcloud.android.stations;

import butterknife.ButterKnife;
import butterknife.InjectView;
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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class StationDetailsPresenter extends RecyclerViewPresenter<Station> {

    private final StationsOperations operations;
    private final StationDetailsAdapter adapter;
    private final Resources resources;
    @InjectView(R.id.ak_recycler_view) RecyclerView recyclerView;
    private Observable<List<Station>> source;

    @Inject
    public StationDetailsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                   StationsOperations operations,
                                   StationDetailsAdapter adapter,
                                   Resources resources) {
        super(swipeRefreshAttacher, Options.cards());
        this.operations = operations;
        this.adapter = adapter;
        this.resources = resources;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
        source = stationsSource(bundle);
    }

    private Observable<List<Station>> stationsSource(Bundle bundle) {
        // TODO Chose the source (recent saved, ...) depending on a bundle argument.
        return operations.recentStations().toList();
    }

    @Override
    protected CollectionBinding<Station> onBuildBinding(Bundle bundle) {
        return CollectionBinding.from(source)
                .withAdapter(adapter)
                .build();
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

    static class StationDetailsAdapter extends RecyclerItemAdapter<Station, StationDetailsAdapter.StationViewHolder> {
        private static final int STATION_TYPE = 0;

        @Inject
        StationDetailsAdapter(StationRenderer renderer) {
            super(renderer);
        }

        @Override
        protected StationViewHolder createViewHolder(View view) {
            return new StationViewHolder(view);
        }

        @Override
        public int getBasicItemViewType(int i) {
            return STATION_TYPE;
        }

        public static class StationViewHolder extends RecyclerView.ViewHolder {
            public StationViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
