package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.tracks.NowPlayingAdapter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleBinder;
import rx.Observable;
import rx.functions.Func1;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class StationsHomePresenter extends RecyclerViewPresenter<StationBucket> {
    private final Func1<SyncResult, Observable<List<StationBucket>>> toBuckets = new Func1<SyncResult, Observable<List<StationBucket>>>() {
        @Override
        public Observable<List<StationBucket>> call(SyncResult ignored) {
            return buckets();
        }
    };

    private final Func1<List<StationViewModel>, Boolean> hasStations = new Func1<List<StationViewModel>, Boolean>() {
        @Override
        public Boolean call(List<StationViewModel> stations) {
            return !stations.isEmpty();
        }
    };

    private final Resources resources;
    private final StationsOperations operations;
    private final StationsHomeAdapter adapter;
    @LightCycle final StationsNowPlayingController stationsNowPlayingController;
    private final PlayQueueManager playQueueManager;

    @Inject
    public StationsHomePresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                 Resources resources,
                                 StationsOperations operations,
                                 StationsHomeAdapter adapter,
                                 StationsNowPlayingController stationsNowPlayingController,
                                 PlayQueueManager playQueueManager) {
        super(swipeRefreshAttacher, Options.defaults());
        this.resources = resources;
        this.operations = operations;
        this.adapter = adapter;
        this.stationsNowPlayingController = stationsNowPlayingController;
        this.playQueueManager = playQueueManager;
        this.stationsNowPlayingController.setAdapter(adapter);
        LightCycleBinder.bind(this);
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
                bucket(StationsCollectionsTypes.RECENT, resources.getString(R.string.stations_collection_title_recently_played_stations)),
                bucket(StationsCollectionsTypes.SAVED, resources.getString(R.string.stations_collection_title_saved_stations)),
                bucket(StationsCollectionsTypes.TRACK_RECOMMENDATIONS, resources.getString(R.string.stations_collection_title_track_recommendations)),
                bucket(StationsCollectionsTypes.GENRE_RECOMMENDATIONS, resources.getString(R.string.stations_collection_title_genre_recommendations)),
                bucket(StationsCollectionsTypes.CURATOR_RECOMMENDATIONS, resources.getString(R.string.stations_collection_title_curator_recommendations)))
                .toList();
    }

    private Observable<StationBucket> bucket(int type, String title) {
        final Func1<Station, StationViewModel> toViewModel = buildToViewModel(playQueueManager.getCollectionUrn());

        return operations
                .collection(type)
                .map(toViewModel)
                .toList()
                .filter(hasStations)
                .map(StationBucket.fromStationViewModels(title, type, maxStationsPerBucket()));
    }

    private Func1<Station, StationViewModel> buildToViewModel(final Urn currentlyPlayingCollection) {
        return new Func1<Station, StationViewModel>() {
            @Override
            public StationViewModel call(Station station) {
                return new StationViewModel(station, currentlyPlayingCollection.equals(station.getUrn()));
            }
        };
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

    static class StationsHomeAdapter extends RecyclerItemAdapter<StationBucket, StationsViewHolder> implements NowPlayingAdapter {

        public static final int STATION_BUCKET_TYPE = 0;

        @Inject
        public StationsHomeAdapter(StationsBucketRenderer renderer) {
            super(renderer);
        }

        @Override
        public void updateNowPlaying(Urn currentlyPlayingCollectionUrn) {
            for (StationBucket stationBucket : getItems()) {
                for (StationViewModel stationViewModel : stationBucket.getStationViewModels()) {
                    stationViewModel.setIsPlaying(stationViewModel.getStation().getUrn().equals(currentlyPlayingCollectionUrn));
                }
            }

            notifyDataSetChanged();
        }

        @Override
        protected StationsViewHolder createViewHolder(View view) {
            return new StationsViewHolder(view);
        }

        @Override
        public int getBasicItemViewType(int i) {
            return STATION_BUCKET_TYPE;
        }
    }
}
