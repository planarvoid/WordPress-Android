package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;
import static com.soundcloud.android.events.EventQueue.REPOST_CHANGED;
import static com.soundcloud.android.events.EventQueue.TRACK_CHANGED;
import static com.soundcloud.android.feedback.Feedback.LENGTH_LONG;
import static com.soundcloud.android.rx.observers.DefaultObserver.onNext;
import static com.soundcloud.java.collections.MoreCollections.transform;

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
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.RefreshRecyclerViewAdapterSubscriber;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
import com.soundcloud.android.view.adapters.RepostEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateTrackListSubscriber;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Function;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

class PlayHistoryPresenter extends RecyclerViewPresenter<List<PlayHistoryItem>, PlayHistoryItem>
        implements SimpleHeaderRenderer.Listener, ClearPlayHistoryDialog.Listener, TrackItemRenderer.Listener {

    private static final Function<TrackItem, PlayHistoryItem> TRACK_TO_PLAY_HISTORY_ITEM = trackItem -> PlayHistoryItemTrack.create(trackItem);

    private final PlayHistoryOperations playHistoryOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PlayHistoryAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    private CompositeSubscription viewLifeCycle;
    private Fragment fragment;
    private FeedbackController feedbackController;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final FeatureFlags featureFlags;

    @Inject
    PlayHistoryPresenter(PlayHistoryOperations playHistoryOperations,
                         OfflineContentOperations offlineContentOperations,
                         PlayHistoryAdapter adapter,
                         Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                         EventBus eventBus,
                         SwipeRefreshAttacher swipeRefreshAttacher,
                         FeedbackController feedbackController,
                         OfflinePropertiesProvider offlinePropertiesProvider,
                         FeatureFlags featureFlags,
                         PerformanceMetricsEngine performanceMetricsEngine) {
        super(swipeRefreshAttacher);
        this.playHistoryOperations = playHistoryOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        this.feedbackController = feedbackController;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.featureFlags = featureFlags;
        this.performanceMetricsEngine = performanceMetricsEngine;

        adapter.setMenuClickListener(this);
        adapter.setTrackClickListener(this);
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onClearConfirmationClicked() {
        playHistoryOperations.clearHistory().subscribe(new ClearHistorySubscriber());
    }

    @Override
    public void onClearClicked() {
        new ClearPlayHistoryDialog().setListener(this).show(fragment.getFragmentManager());
    }

    @Override
    public void trackItemClicked(Urn urn, int position) {
        playHistoryOperations
                .startPlaybackFrom(urn, Screen.PLAY_HISTORY)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    @Override
    protected CollectionBinding<List<PlayHistoryItem>, PlayHistoryItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(playHistoryOperations.playHistory()
                                                           .map(toPlayHistoryItem()))
                                .withAdapter(adapter)
                                .addObserver(onNext(items -> endMeasuringListeningHistoryLoad(Iterables.size(items))))
                                .build();
    }

    private void endMeasuringListeningHistoryLoad(int historySize) {
        MetricParams metricParams = new MetricParams().putLong(MetricKey.LISTENING_HISTORY_SIZE, historySize);
        performanceMetricsEngine.endMeasuring(PerformanceMetric.builder()
                                                               .metricType(MetricType.LISTENING_HISTORY_LOAD)
                                                               .metricParams(metricParams)
                                                               .build());
    }

    private Func1<List<TrackItem>, List<PlayHistoryItem>> toPlayHistoryItem() {
        return trackItems -> {
            final int trackCount = trackItems.size();
            final List<PlayHistoryItem> items = new ArrayList<>(trackCount + 1);

            if (trackCount > 0) {
                items.add(PlayHistoryItemHeader.create(trackCount));
                items.addAll(transform(trackItems, TRACK_TO_PLAY_HISTORY_ITEM));
            }

            return items;
        };
    }

    @Override
    protected CollectionBinding<List<PlayHistoryItem>, PlayHistoryItem> onRefreshBinding() {
        return CollectionBinding.from(playHistoryOperations.refreshPlayHistory().map(toPlayHistoryItem()))
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        this.fragment = fragment;

        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)),
                subscribeToOfflineContent(),
                eventBus.subscribe(TRACK_CHANGED, new UpdateTrackListSubscriber(adapter)),
                eventBus.subscribe(LIKE_CHANGED, new LikeEntityListSubscriber(adapter)),
                eventBus.subscribe(REPOST_CHANGED, new RepostEntityListSubscriber(adapter)),
                offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()
                                        .subscribe(new RefreshRecyclerViewAdapterSubscriber(adapter))
        );

        getEmptyView().setImage(R.drawable.collection_empty_playlists);
        getEmptyView().setMessageText(R.string.collections_play_history_empty);
        getEmptyView().setBackgroundResource(R.color.page_background);
    }

    private Subscription subscribeToOfflineContent() {
        if (featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)) {
            return offlinePropertiesProvider.states()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(new OfflinePropertiesSubscriber());
        } else {
            return eventBus.queue(OFFLINE_CONTENT_CHANGED)
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe(new CurrentDownloadSubscriber());
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        this.fragment = null;
        super.onDestroyView(fragment);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private class ClearHistorySubscriber extends DefaultSubscriber<Boolean> {
        @Override
        public void onNext(Boolean wasSuccessful) {
            if (!wasSuccessful) {
                Feedback feedback = Feedback.create(R.string.collections_play_history_clear_error_message, LENGTH_LONG);
                feedbackController.showFeedback(feedback);
            } else {
                adapter.clear();
                retryWith(onBuildBinding(null));
                eventBus.publish(EventQueue.PLAY_HISTORY, PlayHistoryEvent.updated());
            }
        }
    }

    private abstract class UpdateSubscriber<T> extends DefaultSubscriber<T> {

        abstract TrackItem getUpdatedTrackItem(T event, TrackItem itemTrack);

        abstract boolean containsTrackUrn(T event, Urn urn);

        @Override
        public void onNext(final T event) {
            final int itemCount = adapter.getItemCount();

            for (int position = 0; position < itemCount; position++) {
                final PlayHistoryItem item = adapter.getItem(position);

                if (item.getKind() == PlayHistoryItem.Kind.PlayHistoryTrack) {
                    final TrackItem trackItem = ((PlayHistoryItemTrack) item).trackItem();
                    final Urn trackUrn = trackItem.getUrn();

                    if (containsTrackUrn(event, trackUrn) && adapter.getItems().size() > position) {
                        final PlayHistoryItemTrack updatedItem = PlayHistoryItemTrack.create(getUpdatedTrackItem(event, trackItem));
                        adapter.setItem(position, updatedItem);
                    }
                }
            }
        }
    }

    class OfflinePropertiesSubscriber extends UpdateSubscriber<OfflineProperties> {

        @Override
        TrackItem getUpdatedTrackItem(OfflineProperties properties, TrackItem trackItem) {
            final OfflineState offlineState = properties.state(trackItem.getUrn());
            return trackItem.updatedWithOfflineState(offlineState);
        }

        @Override
        boolean containsTrackUrn(OfflineProperties event, Urn urn) {
            // OfflineProperties contains everything or fallback to not offline.
            // It is not an update, it's a snapshot.
            return true;
        }
    }

    private class CurrentDownloadSubscriber extends UpdateSubscriber<OfflineContentChangedEvent> {
        @Override
        TrackItem getUpdatedTrackItem(OfflineContentChangedEvent event, TrackItem trackItem) {
            return trackItem.updatedWithOfflineState(event.state);
        }

        @Override
        boolean containsTrackUrn(OfflineContentChangedEvent event, Urn urn) {
            return event.entities.contains(urn);
        }
    }
}
