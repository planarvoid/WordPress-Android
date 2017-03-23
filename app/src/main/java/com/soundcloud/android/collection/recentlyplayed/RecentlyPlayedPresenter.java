package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.android.feedback.Feedback.LENGTH_LONG;
import static com.soundcloud.android.rx.observers.LambdaSubscriber.onNext;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class RecentlyPlayedPresenter extends RecyclerViewPresenter<List<RecentlyPlayedItem>, RecentlyPlayedItem>
        implements ClearRecentlyPlayedDialog.Listener, SimpleHeaderRenderer.Listener {

    private final RecentlyPlayedAdapter adapter;
    private final Resources resources;
    private final RecentlyPlayedOperations recentlyPlayedOperations;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final FeatureFlags featureFlags;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private Fragment fragment;
    private FeedbackController feedbackController;
    private EventBus eventBus;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public RecentlyPlayedPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                   RecentlyPlayedAdapterFactory adapterFactory,
                                   Resources resources,
                                   RecentlyPlayedOperations recentlyPlayedOperations,
                                   FeedbackController feedbackController,
                                   EventBus eventBus,
                                   OfflinePropertiesProvider offlinePropertiesProvider,
                                   FeatureFlags featureFlags,
                                   PerformanceMetricsEngine performanceMetricsEngine) {
        super(swipeRefreshAttacher, new Options.Builder().useDividers(Options.DividerMode.NONE).build());
        this.adapter = adapterFactory.create(false, this);
        this.resources = resources;
        this.recentlyPlayedOperations = recentlyPlayedOperations;
        this.feedbackController = feedbackController;
        this.eventBus = eventBus;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.featureFlags = featureFlags;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        this.fragment = null;
        subscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onClearConfirmationClicked() {
        recentlyPlayedOperations.clearHistory().subscribe(new ClearSubscriber());
    }

    @Override
    public void onClearClicked() {
        new ClearRecentlyPlayedDialog().setListener(this).show(fragment.getFragmentManager());
    }

    @Override
    protected CollectionBinding<List<RecentlyPlayedItem>, RecentlyPlayedItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(recentlyPlayedOperations.recentlyPlayed().map(augmentRecentlyPlayedItems()))
                                .withAdapter(adapter)
                                .addObserver(onNext(items -> endMeasuringLoadTime(Iterables.size(items))))
                                .build();
    }

    private void endMeasuringLoadTime(int itemsCount) {
        MetricParams params = new MetricParams().putLong(MetricKey.RECENTLY_PLAYED_SIZE, itemsCount);
        performanceMetricsEngine.endMeasuring(PerformanceMetric.builder()
                                                               .metricType(MetricType.RECENTLY_PLAYED_LOAD)
                                                               .metricParams(params)
                                                               .build());
    }

    @Override
    protected CollectionBinding<List<RecentlyPlayedItem>, RecentlyPlayedItem> onRefreshBinding() {
        return CollectionBinding.from(recentlyPlayedOperations.refreshRecentlyPlayed()
                                                              .map(augmentRecentlyPlayedItems()))
                                .withAdapter(adapter)
                                .build();
    }

    private Func1<List<RecentlyPlayedPlayableItem>, List<RecentlyPlayedItem>> augmentRecentlyPlayedItems() {
        return recentlyPlayedPlayableItems -> {
            final int contextCount = recentlyPlayedPlayableItems.size();
            List<RecentlyPlayedItem> list = new ArrayList<>(contextCount + 1);

            if (contextCount > 0) {
                list.add(RecentlyPlayedHeader.create(contextCount));
                list.addAll(recentlyPlayedPlayableItems);
            }
            return list;
        };
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        this.fragment = fragment;

        setupRecyclerView(view);
        setupEmptyView();
        subscribeToOfflineContent();
    }

    private void subscribeToOfflineContent() {
        if (featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)) {
            subscription = offlinePropertiesProvider.states()
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(new OfflineContentSubscriber(adapter));
        } else {
            subscription = eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new CurrentDownloadSubscriber(adapter));
        }
    }

    private void setupRecyclerView(View view) {
        final int spanCount = resources.getInteger(R.integer.collection_grid_span_count);
        final int itemMargin = view.getResources().getDimensionPixelSize(R.dimen.collection_padding);
        final GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), spanCount);
        final RecyclerView recyclerView = getRecyclerView();

        layoutManager.setSpanSizeLookup(getSpanSizeLookup(spanCount));

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new RecentlyPlayedItemDecoration(itemMargin));
        recyclerView.setPadding(itemMargin, 0, itemMargin, 0);
        recyclerView.setClipToPadding(false);
    }

    private GridLayoutManager.SpanSizeLookup getSpanSizeLookup(final int spanCount) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                final RecentlyPlayedItem item = adapter.getItem(position);
                if (item.getKind() == RecentlyPlayedItem.Kind.RecentlyPlayedHeader) {
                    return spanCount;
                } else {
                    return 1;
                }
            }
        };
    }

    private void setupEmptyView() {
        getEmptyView().setImage(R.drawable.collection_emtpy_recently_played);
        getEmptyView().setMessageText(R.string.collections_recently_played_empty);
        getEmptyView().setBackgroundResource(R.color.page_background);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private static class RecentlyPlayedItemDecoration extends RecyclerView.ItemDecoration {

        private final int spacing;

        RecentlyPlayedItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) view.getLayoutParams();
            boolean isSingleSpan = layoutParams.getSpanSize() == 1;

            outRect.left = isSingleSpan ? 0 : -spacing;
            outRect.right = isSingleSpan ? 0 : -spacing;
            outRect.bottom = isSingleSpan ? spacing : 0;
            outRect.top = isSingleSpan ? spacing : 0;
        }
    }

    private class ClearSubscriber extends DefaultSubscriber<Boolean> {
        @Override
        public void onNext(Boolean wasSuccessful) {
            if (!wasSuccessful) {
                feedbackController.showFeedback(Feedback.create(R.string.collections_recently_played_clear_error_message,
                                                                LENGTH_LONG));
            } else {
                adapter.clear();
                retryWith(onBuildBinding(null));
                eventBus.publish(EventQueue.PLAY_HISTORY, PlayHistoryEvent.updated());
            }
        }
    }

    private static class CurrentDownloadSubscriber extends DefaultSubscriber<OfflineContentChangedEvent> {
        private final RecentlyPlayedAdapter adapter;

        CurrentDownloadSubscriber(RecentlyPlayedAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onNext(OfflineContentChangedEvent event) {
            adapter.updateOfflineState(event);
        }
    }

    private static class OfflineContentSubscriber extends DefaultSubscriber<OfflineProperties> {
        private final RecentlyPlayedAdapter adapter;

        OfflineContentSubscriber(RecentlyPlayedAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onNext(OfflineProperties event) {
            adapter.updateOfflineState(event);
        }
    }
}
