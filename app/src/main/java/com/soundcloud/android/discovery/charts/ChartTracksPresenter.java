package com.soundcloud.android.discovery.charts;


import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

class ChartTracksPresenter extends RecyclerViewPresenter<ApiChart<ApiTrack>, ChartTrackListItem> {

    static final int HEADER_OFFSET = 1;

    private static final int NUM_EXTRA_ITEMS = 2;

    private final PlaySessionStateProvider playSessionStateProvider;
    private final CompositeSubscription subscription = new CompositeSubscription();

    private final Action1<ApiChart<ApiTrack>> trackChart = new Action1<ApiChart<ApiTrack>>() {
        @Override
        public void call(ApiChart<ApiTrack> apiTrackApiChart) {
            final Optional<Urn> queryUrn = apiTrackApiChart.tracks().getQueryUrn();
            if (queryUrn.isPresent()) {
                chartsTracker.chartDataLoaded(queryUrn.get(),
                                              apiTrackApiChart.type(),
                                              apiTrackApiChart.category(),
                                              apiTrackApiChart.genre());
            }
        }
    };

    private final ChartsOperations chartsOperations;
    private final ChartTracksAdapter chartTracksAdapter;
    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final ChartsTracker chartsTracker;
    private final EntityItemCreator entityItemCreator;
    private final PublishSubject<Throwable> errorSubject = PublishSubject.create();
    private final EventBus eventBus;

    @Inject
    ChartTracksPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                         ChartsOperations chartsOperations,
                         ChartTracksAdapter chartTracksAdapter,
                         PlaybackInitiator playbackInitiator,
                         Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                         ChartsTracker chartsTracker,
                         EntityItemCreator entityItemCreator,
                         PlaySessionStateProvider playSessionStateProvider,
                         EventBus eventBus) {
        super(swipeRefreshAttacher);
        this.chartsOperations = chartsOperations;
        this.chartTracksAdapter = chartTracksAdapter;
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.chartsTracker = chartsTracker;
        this.entityItemCreator = entityItemCreator;
        this.playSessionStateProvider = playSessionStateProvider;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onDestroy(Fragment fragment) {
        subscription.unsubscribe();
        super.onDestroy(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        subscription.add(eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(chartTracksAdapter)));
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final ChartTrackListItem chartTrackListItem = chartTracksAdapter.getItem(position);
        if (chartTrackListItem.isTrack()) {
            final ChartTrackItem trackItem = ((ChartTrackListItem.Track) chartTrackListItem).chartTrackItem();
            final List<Urn> playQueue = getPlayQueue();
            final String screen = chartsTracker.getScreen(trackItem.chartType(), trackItem.chartCategory(), trackItem.genre());
            final int queryPosition = position - HEADER_OFFSET;

            final Optional<Urn> queryUrn = trackItem.queryUrn();
            final PlaySessionSource playSessionSource = (queryUrn.isPresent()) ?
                                                        PlaySessionSource.forChart(screen, queryPosition,
                                                                                   queryUrn.get(),
                                                                                   trackItem.chartType(),
                                                                                   trackItem.genre()) :
                                                        PlaySessionSource.EMPTY;

            playbackInitiator.playTracks(playQueue, playQueue.indexOf(trackItem.getUrn()), playSessionSource)
                             .subscribe(expandPlayerSubscriberProvider.get());
        }
    }

    private Func1<ApiChart<ApiTrack>, Iterable<ChartTrackListItem>> toChartTrackListItems() {
        return apiChart -> {
            final List<ChartTrackListItem> chartTrackListItems = new ArrayList<>(apiChart.tracks()
                                                                                         .getCollection()
                                                                                         .size() + NUM_EXTRA_ITEMS);

            chartTrackListItems.add(ChartTrackListItem.Header.create(apiChart.type()));

            for (ApiTrack apiTrack : apiChart.tracks()) {
                final boolean isTrackPlaying = playSessionStateProvider.isCurrentlyPlaying(apiTrack.getUrn());
                final TrackItem trackItem = entityItemCreator.trackItem(apiTrack).withPlayingState(isTrackPlaying);
                ChartTrackItem chartTrackItem = new ChartTrackItem(
                        apiChart.type(),
                        trackItem,
                        apiChart.category(),
                        apiChart.genre(),
                        apiChart.getQueryUrn());
                ChartTrackListItem chartTrackListItem = ChartTrackListItem.Track.create(chartTrackItem);
                chartTrackListItems.add(chartTrackListItem);
            }

            chartTrackListItems.add(ChartTrackListItem.Footer.create(apiChart.lastUpdated()));
            return chartTrackListItems;
        };
    }

    @Override
    protected CollectionBinding<ApiChart<ApiTrack>, ChartTrackListItem> onBuildBinding(Bundle bundle) {
        final ChartType chartType = (ChartType) bundle.getSerializable(ChartTracksFragment.EXTRA_TYPE);
        final Urn chartUrn = bundle.getParcelable(ChartTracksFragment.EXTRA_GENRE_URN);
        final Observable<ApiChart<ApiTrack>> chartTracks = RxJava.toV1Observable(chartsOperations.tracks(chartType, chartUrn.getStringId()));
        return CollectionBinding
                .from(chartTracks.doOnNext(trackChart), toChartTrackListItems())
                .withAdapter(chartTracksAdapter)
                .build();
    }

    Observable<ApiRequestException> invalidGenreError() {
        return errorSubject.filter(throwable -> throwable instanceof ApiRequestException)
                           .cast(ApiRequestException.class)
                           .filter(error -> error.reason().equals(ApiRequestException.Reason.NOT_FOUND));
    }

    private List<Urn> getPlayQueue() {
        final List<Urn> playQueue = new ArrayList<>();
        for (ChartTrackListItem chartTrackListItem : chartTracksAdapter.getItems()) {
            if (chartTrackListItem.isTrack()) {
                final Urn urn = ((ChartTrackListItem.Track) chartTrackListItem).chartTrackItem().getUrn();
                playQueue.add(urn);
            }
        }
        return playQueue;
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        errorSubject.onNext(error);
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
