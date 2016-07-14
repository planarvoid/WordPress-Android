package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.RefreshRecyclerViewAdapterSubscriber;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.PagedTracksRecyclerItemAdapter;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

class PlayHistoryPresenter extends RecyclerViewPresenter<List<TrackItem>, TrackItem> {

    private final PlayHistoryOperations playHistoryOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PagedTracksRecyclerItemAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;

    @Inject
    PlayHistoryPresenter(PlayHistoryOperations playHistoryOperations,
                         OfflineContentOperations offlineContentOperations,
                         PagedTracksRecyclerItemAdapter adapter,
                         Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider, EventBus eventBus,
                         SwipeRefreshAttacher swipeRefreshAttacher) {
        super(swipeRefreshAttacher);
        this.playHistoryOperations = playHistoryOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<TrackItem>, TrackItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(playHistoryOperations.playHistory())
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM,
                                   new UpdatePlayingTrackSubscriber(adapter)),
                eventBus.queue(OFFLINE_CONTENT_CHANGED)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new UpdateCurrentDownloadSubscriber(adapter)),
                eventBus.subscribe(ENTITY_STATE_CHANGED,
                                   new UpdateEntityListSubscriber(adapter)),
                offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()
                                        .subscribe(new RefreshRecyclerViewAdapterSubscriber(adapter))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onItemClicked(View view, int position) {
        TrackItem item = adapter.getItem(position);

        playHistoryOperations
                .startPlaybackFrom(item.getUrn(), Screen.PLAY_HISTORY)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}
