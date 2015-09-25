package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleBinder;
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

class ShowAllStationsPresenter extends RecyclerViewPresenter<StationViewModel> {
    private static final String COLLECTION_TYPE_KEY = "type";

    private final Func1<SyncResult, Observable<List<StationViewModel>>> toStationViewModels = new Func1<SyncResult, Observable<List<StationViewModel>>>() {
        @Override
        public Observable<List<StationViewModel>> call(SyncResult ignored) {
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
    public ShowAllStationsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                    StationsOperations operations,
                                    StationsAdapter adapter,
                                    Resources resources,
                                    PlayQueueManager playQueueManager,
                                    StationsNowPlayingController stationsNowPlayingController) {
        super(swipeRefreshAttacher, Options.cards());
        this.operations = operations;
        this.adapter = adapter;
        this.resources = resources;
        this.playQueueManager = playQueueManager;
        this.stationsNowPlayingController = stationsNowPlayingController;
        this.stationsNowPlayingController.setAdapter(adapter);
        LightCycleBinder.bind(this);
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    public static Bundle createBundle(int collectionType) {
        final Bundle bundle = new Bundle();
        bundle.putInt(COLLECTION_TYPE_KEY, collectionType);
        return bundle;
    }

    private Observable<List<StationViewModel>> stationsSource(Bundle bundle) {
        final Func1<Station, StationViewModel> toViewModel = buildToViewModel();

        return operations
                .collection(getCollectionType(bundle))
                .map(toViewModel)
                .take(resources.getInteger(R.integer.stations_list_max_recent_stations))
                .toList();
    }

    private int getCollectionType(Bundle bundle) {
        return bundle.getInt(COLLECTION_TYPE_KEY);
    }

    private Func1<Station, StationViewModel> buildToViewModel() {
        return new Func1<Station, StationViewModel>() {
            @Override
            public StationViewModel call(Station station) {
                return new StationViewModel(station, playQueueManager.getCollectionUrn().equals(station.getUrn()));
            }
        };
    }

    @Override
    protected CollectionBinding<StationViewModel> onBuildBinding(Bundle bundle) {
        source = stationsSource(bundle);

        return CollectionBinding
                .from(source)
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<StationViewModel> onRefreshBinding() {
        return CollectionBinding
                .from(operations.sync().flatMap(toStationViewModels))
                .withAdapter(adapter)
                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        RecyclerView recyclerView = getRecyclerView();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(), resources.getInteger(R.integer.stations_grid_span_count)));
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
