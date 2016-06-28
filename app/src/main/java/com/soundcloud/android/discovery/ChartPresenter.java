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
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.Lists;
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

class ChartPresenter extends RecyclerViewPresenter<PagedChartTracks, ChartTrackItem> {

    private final static Predicate<ChartTrackItem> isTrack = new Predicate<ChartTrackItem>() {
        public boolean apply(ChartTrackItem input) {
            return input.getKind() == ChartTrackItem.Kind.TrackItem;
        }
    };

    private static final Function<ApiTrack, ChartTrackItem> apiTrackToChartItem = new Function<ApiTrack, ChartTrackItem>() {
        public ChartTrackItem apply(ApiTrack input) {
            return ChartTrackItem.forTrack(TrackItem.from(input));
        }
    };

    private static final Func1<PagedChartTracks, Iterable<ChartTrackItem>> toPresentationModels =
            new Func1<PagedChartTracks, Iterable<ChartTrackItem>>() {
                @Override
                public Iterable<ChartTrackItem> call(PagedChartTracks pagedChartTracks) {

                    final List<ChartTrackItem> chartTrackItems = new ArrayList<>();
                    if (pagedChartTracks.firstPage()) {
                        chartTrackItems.add(ChartTrackItem.forHeader(pagedChartTracks.chartType()));
                    }
                    chartTrackItems.addAll(Lists.newArrayList(transform(pagedChartTracks.items(), apiTrackToChartItem)));
                    if (pagedChartTracks.lastPage()) {
                        chartTrackItems.add(ChartTrackItem.forFooter(pagedChartTracks.lastUpdated()));
                    }
                    return chartTrackItems;
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
        final ChartTrackItem item = chartTrackAdapter.getItem(position);
        if (isTrack.apply(item)) {
            final ChartTrackItem.Track trackItem = (ChartTrackItem.Track) item;
            final List<Urn> playQueue = getPlayQueue();

            playbackInitiator.playTracks(playQueue, playQueue.indexOf(trackItem.trackItem.getUrn()), PlaySessionSource.EMPTY)
                             .subscribe(expandPlayerSubscriberProvider.get());
        }
    }

    @Override
    protected CollectionBinding<PagedChartTracks, ChartTrackItem> onBuildBinding(Bundle bundle) {
        final ChartType chartType = (ChartType) bundle.getSerializable(ChartFragment.EXTRA_TYPE);
        final Urn chartUrn = bundle.getParcelable(ChartFragment.EXTRA_GENRE_URN);
        return CollectionBinding
                .from(chartsOperations.firstPagedTracks(chartType, chartUrn.getStringId()), toPresentationModels)
                .withAdapter(chartTrackAdapter)
                .withPager(chartsOperations.nextPagedTracks())
                .build();
    }

    private List<Urn> getPlayQueue() {
        final List<Urn> playQueue = new ArrayList<>();
        for (ChartTrackItem chartTrackItem : chartTrackAdapter.getItems()) {
            if (isTrack.apply(chartTrackItem)) {
                final Urn urn = ((ChartTrackItem.Track) chartTrackItem).trackItem.getUrn();
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
