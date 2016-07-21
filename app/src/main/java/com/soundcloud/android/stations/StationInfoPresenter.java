package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationInfo.FROM_STATION_RECORD;
import static com.soundcloud.android.stations.StationInfoTracksBucket.FROM_TRACK_ITEM_LIST;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import java.util.List;

class StationInfoPresenter extends RecyclerViewPresenter<List<StationInfoItem>, StationInfoItem>
        implements StationInfoClickListener {

    private final StationTrackOperations stationTrackOperations;
    private final StartStationPresenter stationPresenter;
    private final StationsOperations stationOperations;
    private final StationInfoAdapter adapter;
    private final EventBus eventBus;

    private Subscription subscription = RxUtils.invalidSubscription();
    private Urn stationUrn;

    @Inject
    public StationInfoPresenter(StationTrackOperations stationsTrackOperations,
                                SwipeRefreshAttacher swipeRefreshAttacher,
                                StationInfoAdapterFactory adapterFactory,
                                StationsOperations stationOperations,
                                StartStationPresenter stationPresenter,
                                StationInfoTracksBucketRendererFactory bucketRendererFactory,
                                EventBus eventBus) {
        super(swipeRefreshAttacher);
        this.stationTrackOperations = stationsTrackOperations;
        this.stationOperations = stationOperations;
        this.stationPresenter = stationPresenter;
        this.eventBus = eventBus;
        this.adapter = adapterFactory.create(this, bucketRendererFactory.create(this));
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();

        subscription = eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                                          new UpdatePlayingTrackSubscriber(adapter));
    }

    @Override
    public void onDestroy(Fragment fragment) {
        super.onDestroy(fragment);
        subscription.unsubscribe();
    }

    @Override
    protected CollectionBinding<List<StationInfoItem>, StationInfoItem> onBuildBinding(Bundle fragmentArgs) {
        stationUrn = fragmentArgs.getParcelable(StationInfoFragment.EXTRA_URN);

        return CollectionBinding.from(getStationInfo(stationUrn)
                                              .concatWith(getStationTracks(stationUrn))
                                              .toList())
                                .withAdapter(adapter).build();
    }

    private Observable<StationInfoItem> getStationInfo(Urn stationUrn) {
        return stationOperations.station(stationUrn).map(FROM_STATION_RECORD);
    }

    private Observable<StationInfoItem> getStationTracks(Urn stationUrn) {
        return stationTrackOperations.stationTracks(stationUrn).map(FROM_TRACK_ITEM_LIST);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onPlayButtonClicked(Context context, Urn stationUrn) {
        stationPresenter.startStation(context, stationUrn);
    }

    @Override
    public void onTrackClicked(Context context, int position) {
        stationPresenter.startStationFromPosition(context, stationUrn, position);
    }

}
