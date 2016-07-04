package com.soundcloud.android.discovery;


import static com.soundcloud.java.collections.Iterables.transform;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

class ChartPresenter extends RecyclerViewPresenter<PagedChartTracks, ChartTrackListItem> {

    private static final Predicate<ChartTrackListItem> IS_TRACK = new Predicate<ChartTrackListItem>() {
        public boolean apply(ChartTrackListItem input) {
            return input.getKind() == ChartTrackListItem.Kind.TrackItem;
        }
    };

    private static Function<ApiTrack, ChartTrackListItem> toChartTrackListItem(final ChartType chartType) {
        return new Function<ApiTrack, ChartTrackListItem>() {
            public ChartTrackListItem apply(ApiTrack input) {
                return ChartTrackListItem.forTrack(new ChartTrackItem(chartType, input));
            }
        };
    }

    private static final Func1<PagedChartTracks, Iterable<ChartTrackListItem>> TO_PRESENTATION_MODELS =
            new Func1<PagedChartTracks, Iterable<ChartTrackListItem>>() {
                @Override
                public Iterable<ChartTrackListItem> call(final PagedChartTracks pagedChartTracks) {

                    final List<ChartTrackListItem> chartTrackListItems = new ArrayList<>(pagedChartTracks.items()
                                                                                                         .getCollection()
                                                                                                         .size() + 2);
                    if (pagedChartTracks.firstPage()) {
                        chartTrackListItems.add(ChartTrackListItem.forHeader(pagedChartTracks.chartType()));
                    }
                    Iterables.addAll(chartTrackListItems,
                                     transform(pagedChartTracks.items(),
                                               toChartTrackListItem(pagedChartTracks.chartType())));
                    if (pagedChartTracks.lastPage()) {
                        chartTrackListItems.add(ChartTrackListItem.forFooter(pagedChartTracks.lastUpdated()));
                    }
                    return chartTrackListItems;
                }
            };

    private final ChartsOperations chartsOperations;
    private final ChartTrackAdapter chartTrackAdapter;
    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;

    @Inject
    ChartPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                   ChartsOperations chartsOperations,
                   ChartTrackAdapter chartTrackAdapter,
                   PlaybackInitiator playbackInitiator,
                   Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
        super(swipeRefreshAttacher);
        this.chartsOperations = chartsOperations;
        this.chartTrackAdapter = chartTrackAdapter;
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final ChartTrackListItem item = chartTrackAdapter.getItem(position);
        if (IS_TRACK.apply(item)) {
            final ChartTrackListItem.Track trackItem = (ChartTrackListItem.Track) item;
            final List<Urn> playQueue = getPlayQueue();

            playbackInitiator.playTracks(playQueue, playQueue.indexOf(trackItem.chartTrackItem.getUrn()), PlaySessionSource.EMPTY)
                             .subscribe(expandPlayerSubscriberProvider.get());
        }
    }

    @Override
    protected CollectionBinding<PagedChartTracks, ChartTrackListItem> onBuildBinding(Bundle bundle) {
        final ChartType chartType = (ChartType) bundle.getSerializable(ChartFragment.EXTRA_TYPE);
        final Urn chartUrn = bundle.getParcelable(ChartFragment.EXTRA_GENRE_URN);
        return CollectionBinding
                .from(chartsOperations.firstPagedTracks(chartType, chartUrn.getStringId()), TO_PRESENTATION_MODELS)
                .withAdapter(chartTrackAdapter)
                .withPager(chartsOperations.nextPagedTracks())
                .build();
    }

    private List<Urn> getPlayQueue() {
        final List<Urn> playQueue = new ArrayList<>();
        for (ChartTrackListItem chartTrackListItem : chartTrackAdapter.getItems()) {
            if (IS_TRACK.apply(chartTrackListItem)) {
                final Urn urn = ((ChartTrackListItem.Track) chartTrackListItem).chartTrackItem.getUrn();
                playQueue.add(urn);
            }
        }
        return playQueue;
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
