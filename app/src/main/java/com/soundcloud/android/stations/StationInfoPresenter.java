package com.soundcloud.android.stations;

import static com.soundcloud.android.playback.DiscoverySource.STATIONS;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.stations.StationInfoFragment.EXTRA_SOURCE;
import static com.soundcloud.android.stations.StationInfoFragment.EXTRA_URN;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class StationInfoPresenter extends RecyclerViewPresenter<List<StationInfoItem>, StationInfoItem>
        implements StationInfoClickListener {

    private final StartStationPresenter stationPresenter;
    private final StationsOperations stationOperations;
    private final StationInfoAdapter adapter;
    private final PlayQueueManager playQueueManager;

    private final EventBus eventBus;
    private Subscription subscription = RxUtils.invalidSubscription();
    private DiscoverySource discoverySource;
    private Optional<Urn> seedTrack;
    private Urn stationUrn;

    @Inject
    StationInfoPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                         StationInfoAdapterFactory adapterFactory,
                         StationsOperations stationOperations,
                         StartStationPresenter stationPresenter,
                         StationInfoTracksBucketRendererFactory bucketRendererFactory,
                         PlayQueueManager playQueueManager,
                         EventBus eventBus) {
        super(swipeRefreshAttacher);
        this.stationOperations = stationOperations;
        this.stationPresenter = stationPresenter;
        this.playQueueManager = playQueueManager;
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
        discoverySource = getDiscoverySource(fragmentArgs);
        seedTrack = getSeedTrackUrn(fragmentArgs);
        stationUrn = fragmentArgs.getParcelable(EXTRA_URN);

        return CollectionBinding.from(getStation(stationUrn)).withAdapter(adapter).build();
    }

    private DiscoverySource getDiscoverySource(Bundle fragmentArgs) {
        return DiscoverySource.from(fragmentArgs.getString(EXTRA_SOURCE, STATIONS.value()));
    }

    private Optional<Urn> getSeedTrackUrn(Bundle fragmentArgs) {
        return Optional.fromNullable(fragmentArgs.getParcelable(StationInfoFragment.EXTRA_SEED_TRACK));
    }

    private Observable<List<StationInfoItem>> getStation(final Urn stationUrn) {
        return stationOperations.stationWithTracks(stationUrn, seedTrack)
                                .map(stationWithTracks -> Arrays.asList(StationInfoHeader.from(stationWithTracks), createTracksBucketViewModel(stationWithTracks)));
    }

    private StationInfoTracksBucket createTracksBucketViewModel(StationWithTracks station) {
        final Urn nowPlayingCollection = playQueueManager.getCollectionUrn();
        final Urn nowPlayingUrn = playQueueManager.getCurrentPlayQueueItem().getUrnOrNotSet();

        if (nowPlayingCollection.equals(stationUrn) && !Urn.NOT_SET.equals(nowPlayingUrn)) {
            return StationInfoTracksBucket.from(station, nowPlayingUrn);
        } else {
            return StationInfoTracksBucket.from(station);
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onPlayButtonClicked(Context context) {
        stationPresenter.startStation(context, stationUrn, discoverySource);
    }

    @Override
    public void onTrackClicked(Context context, int position) {
        stationPresenter.startStation(context,
                                      stationOperations.station(stationUrn),
                                      discoverySource, position);
    }

    @Override
    public void onLikeToggled(Context context, boolean liked) {
        fireAndForget(stationOperations.toggleStationLike(stationUrn, liked));
    }

}
