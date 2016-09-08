package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycles;
import rx.Observable;
import rx.functions.Func1;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class RecentStationsPresenter extends RecyclerViewPresenter<List<StationViewModel>, StationViewModel> {
    private final Func1<SyncJobResult, Observable<List<StationViewModel>>> toStationViewModels = new Func1<SyncJobResult, Observable<List<StationViewModel>>>() {
        @Override
        public Observable<List<StationViewModel>> call(SyncJobResult ignored) {
            return source;
        }
    };

    private final StationsOperations operations;
    private final StationsAdapter adapter;
    private final Resources resources;
    private final PlayQueueManager playQueueManager;
    @LightCycle final StationsNowPlayingController stationsNowPlayingController;

    private Observable<List<StationViewModel>> source;

    @Inject
    public RecentStationsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                   StationsOperations operations,
                                   StationsAdapter adapter,
                                   Resources resources,
                                   PlayQueueManager playQueueManager,
                                   StationsNowPlayingController stationsNowPlayingController) {
        super(swipeRefreshAttacher, Options.defaults());
        this.operations = operations;
        this.adapter = adapter;
        this.resources = resources;
        this.playQueueManager = playQueueManager;
        this.stationsNowPlayingController = stationsNowPlayingController;
        this.stationsNowPlayingController.setAdapter(adapter);
        LightCycles.bind(this);
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    private Observable<List<StationViewModel>> stationsSource() {
        final Func1<StationRecord, StationViewModel> toViewModel = buildToViewModel();

        return operations
                .collection(StationsCollectionsTypes.RECENT)
                .map(toViewModel)
                .take(resources.getInteger(R.integer.stations_list_max_recent_stations))
                .toList();
    }

    private Func1<StationRecord, StationViewModel> buildToViewModel() {
        return new Func1<StationRecord, StationViewModel>() {
            @Override
            public StationViewModel call(StationRecord station) {
                return new StationViewModel(station, playQueueManager.getCollectionUrn().equals(station.getUrn()));
            }
        };
    }

    @Override
    protected CollectionBinding<List<StationViewModel>, StationViewModel> onBuildBinding(Bundle bundle) {
        source = stationsSource();

        return CollectionBinding
                .from(source)
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<List<StationViewModel>, StationViewModel> onRefreshBinding() {
        return CollectionBinding
                .from(operations.syncRecentStations().flatMap(toStationViewModels))
                .withAdapter(adapter)
                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        RecyclerView recyclerView = getRecyclerView();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(),
                                                            resources.getInteger(R.integer.stations_grid_span_count)));

        final EmptyView emptyView = getEmptyView();
        emptyView.setMessageText(R.string.recent_stations_empty_view_heading);
        emptyView.setSecondaryText(R.string.recent_stations_empty_view_message);
        emptyView.setImage(R.drawable.empty_stations);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
