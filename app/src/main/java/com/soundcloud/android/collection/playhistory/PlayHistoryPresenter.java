package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;
import static com.soundcloud.android.feedback.Feedback.LENGTH_LONG;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.RefreshRecyclerViewAdapterSubscriber;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
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

    private static final Function<TrackItem, PlayHistoryItem> TRACK_TO_PLAY_HISTORY_ITEM = new Function<TrackItem, PlayHistoryItem>() {
        public PlayHistoryItem apply(TrackItem trackItem) {
            return PlayHistoryItemTrack.create(trackItem);
        }
    };

    private final PlayHistoryOperations playHistoryOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PlayHistoryAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;
    private Fragment fragment;
    private FeedbackController feedbackController;

    @Inject
    PlayHistoryPresenter(PlayHistoryOperations playHistoryOperations,
                         OfflineContentOperations offlineContentOperations,
                         PlayHistoryAdapter adapter,
                         Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                         EventBus eventBus,
                         SwipeRefreshAttacher swipeRefreshAttacher,
                         FeedbackController feedbackController) {
        super(swipeRefreshAttacher);
        this.playHistoryOperations = playHistoryOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        this.feedbackController = feedbackController;

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
        return CollectionBinding.from(playHistoryOperations.playHistory().map(toPlayHistoryItem()))
                                .withAdapter(adapter)
                                .build();
    }

    private Func1<List<TrackItem>, List<PlayHistoryItem>> toPlayHistoryItem() {
        return new Func1<List<TrackItem>, List<PlayHistoryItem>>() {
            @Override
            public List<PlayHistoryItem> call(List<TrackItem> trackItems) {
                final int trackCount = trackItems.size();
                final List<PlayHistoryItem> items = new ArrayList<>(trackCount + 1);

                if (trackCount > 0) {
                    items.add(PlayHistoryItemHeader.create(trackCount));
                    items.addAll(transform(trackItems, TRACK_TO_PLAY_HISTORY_ITEM));
                }

                return items;
            }
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
                eventBus.queue(OFFLINE_CONTENT_CHANGED)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CurrentDownloadSubscriber()),
                eventBus.subscribe(ENTITY_STATE_CHANGED, new EntityStateChangedSubscriber()),
                offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()
                                        .subscribe(new RefreshRecyclerViewAdapterSubscriber(adapter))
        );

        getEmptyView().setImage(R.drawable.collection_empty_playlists);
        getEmptyView().setMessageText(R.string.collections_play_history_empty);
        getEmptyView().setBackgroundResource(R.color.page_background);
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

        abstract PropertySet getChangeSet(T event, Urn urn);

        abstract boolean containsTrackUrn(T event, Urn urn);

        @Override
        public void onNext(final T event) {
            final int itemCount = adapter.getItemCount();

            for (int position = 0; position < itemCount; position++) {
                final PlayHistoryItem item = adapter.getItem(position);

                if (item.getKind() == PlayHistoryItem.Kind.PlayHistoryTrack) {
                    final TrackItem trackItem = ((PlayHistoryItemTrack) item).trackItem();
                    final Urn trackUrn = trackItem.getUrn();

                    if (containsTrackUrn(event, trackUrn)) {
                        trackItem.update(getChangeSet(event, trackUrn));
                        adapter.notifyItemChanged(position);
                    }
                }
            }
        }
    }

    private class CurrentDownloadSubscriber extends UpdateSubscriber<OfflineContentChangedEvent> {
        @Override
        PropertySet getChangeSet(OfflineContentChangedEvent event, Urn urn) {
            return PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(event.state));
        }

        @Override
        boolean containsTrackUrn(OfflineContentChangedEvent event, Urn urn) {
            return event.entities.contains(urn);
        }
    }

    private class EntityStateChangedSubscriber extends UpdateSubscriber<EntityStateChangedEvent> {
        @Override
        PropertySet getChangeSet(EntityStateChangedEvent event, Urn urn) {
            return event.getChangeMap().get(urn);
        }

        @Override
        boolean containsTrackUrn(EntityStateChangedEvent event, Urn urn) {
            return event.getChangeMap().containsKey(urn);
        }
    }

}
