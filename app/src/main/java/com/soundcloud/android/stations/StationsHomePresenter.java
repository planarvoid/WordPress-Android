package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import rx.Observable;
import rx.functions.Func1;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class StationsHomePresenter extends RecyclerViewPresenter<StationBucket> {
    private final Func1<SyncResult, Observable<List<StationBucket>>> toBuckets = new Func1<SyncResult, Observable<List<StationBucket>>>() {
        @Override
        public Observable<List<StationBucket>> call(SyncResult ignored) {
            return buckets();
        }
    };

    private final Resources resources;
    private final StationsOperations operations;
    private final StationsHomeAdapter adapter;
    private final Func1<List<Station>, Boolean> hasStations = new Func1<List<Station>, Boolean>() {
        @Override
        public Boolean call(List<Station> stations) {
            return !stations.isEmpty();
        }
    };

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
                .from(operations.sync().flatMap(toBuckets))
                .withAdapter(adapter)
                .build();
    }

    private Observable<List<StationBucket>> buckets() {
        return Observable.concat(
                bucket(StationsCollectionsTypes.SAVED, resources.getString(R.string.stations_collection_title_saved_stations)),
                bucket(StationsCollectionsTypes.RECENT, resources.getString(R.string.stations_collection_title_recently_played_stations)),
                bucket(StationsCollectionsTypes.TRACK_RECOMMENDATIONS, resources.getString(R.string.stations_collection_title_track_recommendations)),
                bucket(StationsCollectionsTypes.GENRE_RECOMMENDATIONS, resources.getString(R.string.stations_collection_title_genre_recommendations)),
                bucket(StationsCollectionsTypes.CURATOR_RECOMMENDATIONS, resources.getString(R.string.stations_collection_title_curator_recommendations)))
                .toList();
    }

    private Observable<StationBucket> bucket(int type, String title) {
        return operations
                .stations(type)
                .toList()
                .filter(hasStations)
                .map(StationBucket.fromStations(title, type, maxStationsPerBucket()));
    }

    private int maxStationsPerBucket() {
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
