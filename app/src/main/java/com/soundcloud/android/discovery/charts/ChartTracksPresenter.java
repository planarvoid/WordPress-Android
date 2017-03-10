package com.soundcloud.android.discovery.charts;


import static com.soundcloud.java.collections.Iterables.transform;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

class ChartTracksPresenter extends RecyclerViewPresenter<ApiChart<ApiTrack>, ChartTrackListItem> {

    static final int NUM_EXTRA_ITEMS = 1;
    private static final Predicate<ChartTrackListItem> IS_TRACK = input -> input.getKind() == ChartTrackListItem.Kind.TrackItem;

    private Function<ApiTrack, ChartTrackListItem> toChartTrackListItem(final ApiChart<ApiTrack> apiChart) {
        return input -> ChartTrackListItem.forTrack(new ChartTrackItem(apiChart.type(),
                                                                       entityItemCreator.trackItem(input),
                                                                       apiChart.category(),
                                                                       apiChart.genre(),
                                                                       apiChart.getQueryUrn()));
    }

    private final Func1<ApiChart<ApiTrack>, Iterable<ChartTrackListItem>> TO_PRESENTATION_MODELS =
            apiChart -> {

                final List<ChartTrackListItem> chartTrackListItems = new ArrayList<>(apiChart.tracks()
                                                                                             .getCollection()
                                                                                             .size() + 2);
                chartTrackListItems.add(ChartTrackListItem.forHeader(apiChart.type()));
                Iterables.addAll(chartTrackListItems, transform(apiChart.tracks(), toChartTrackListItem(apiChart)));
                chartTrackListItems.add(ChartTrackListItem.forFooter(apiChart.lastUpdated()));
                return chartTrackListItems;
            };

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

    @Inject
    ChartTracksPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                         ChartsOperations chartsOperations,
                         ChartTracksAdapter chartTracksAdapter,
                         PlaybackInitiator playbackInitiator,
                         Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                         ChartsTracker chartsTracker,
                         EntityItemCreator entityItemCreator) {
        super(swipeRefreshAttacher);
        this.chartsOperations = chartsOperations;
        this.chartTracksAdapter = chartTracksAdapter;
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.chartsTracker = chartsTracker;
        this.entityItemCreator = entityItemCreator;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final ChartTrackListItem chartTrackListItem = chartTracksAdapter.getItem(position);
        if (IS_TRACK.apply(chartTrackListItem)) {
            final ChartTrackItem trackItem = ((ChartTrackListItem.Track) chartTrackListItem).chartTrackItem;
            final List<Urn> playQueue = getPlayQueue();
            final String screen = chartsTracker.getScreen(trackItem.chartType(), trackItem.chartCategory(), trackItem.genre());
            final int queryPosition = position - NUM_EXTRA_ITEMS;
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

    @Override
    protected CollectionBinding<ApiChart<ApiTrack>, ChartTrackListItem> onBuildBinding(Bundle bundle) {
        final ChartType chartType = (ChartType) bundle.getSerializable(ChartTracksFragment.EXTRA_TYPE);
        final Urn chartUrn = bundle.getParcelable(ChartTracksFragment.EXTRA_GENRE_URN);
        return CollectionBinding
                .from(chartsOperations.tracks(chartType, chartUrn.getStringId()).doOnNext(trackChart),
                      TO_PRESENTATION_MODELS)
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
            if (IS_TRACK.apply(chartTrackListItem)) {
                final Urn urn = ((ChartTrackListItem.Track) chartTrackListItem).chartTrackItem.getUrn();
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
