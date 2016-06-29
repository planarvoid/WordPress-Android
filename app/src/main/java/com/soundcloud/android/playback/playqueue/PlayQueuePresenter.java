package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.ApplicationModule.LIGHT_TRACK_ADAPTER;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.view.adapters.TracksRecyclerItemAdapter;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class PlayQueuePresenter extends SupportFragmentLightCycleDispatcher<Fragment>
        implements TrackItemRenderer.Listener {

    private final TracksRecyclerItemAdapter adapter;
    private final PlayQueueManager playQueueManager;
    private final PlayQueueOperations playQueueOperations;
    private final EventBus eventBus;
    private final CompositeSubscription subscriptions = new CompositeSubscription();
    private Subscription updateSubscription = RxUtils.invalidSubscription();

    @Bind(R.id.recycler_view) RecyclerView recyclerView;
    @Bind(R.id.play_queue_drawer) View playQueueDrawer;

    @Inject
    public PlayQueuePresenter(@Named(LIGHT_TRACK_ADAPTER) TracksRecyclerItemAdapter adapter,
                              PlayQueueManager playQueueManager,
                              PlayQueueOperations playQueueOperations,
                              EventBus eventBus) {
        this.adapter = adapter;
        this.playQueueManager = playQueueManager;
        this.playQueueOperations = playQueueOperations;
        this.eventBus = eventBus;
        adapter.setTrackItemListener(this);
    }

    @Override
    public void onViewCreated(final Fragment fragment, final View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.bind(this, view);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        playQueueDrawer.setVisibility(View.VISIBLE);
        subscriptions.add(eventBus.subscribeImmediate(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                                                      new UpdatePlayingTrackSubscriber(adapter)));
        subscriptions.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new UpdateSubscriber()));
        refreshPlayQueue();
    }

    @OnClick(R.id.close_play_queue)
    public void closePlayQueue(View view) {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayQueueHidden());
    }

    private class UpdateSubscriber extends DefaultSubscriber<PlayQueueEvent> {

        @Override
        public void onNext(PlayQueueEvent playQueueEvent) {
            updateSubscription.unsubscribe();
            refreshPlayQueue();
        }

    }

    private void refreshPlayQueue() {
        updateSubscription = playQueueOperations.getTrackItems()
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(new PlayQueueSubscriber());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<List<TrackItem>> {

        @Override
        public void onNext(List<TrackItem> trackItems) {
            adapter.clear();
            for (TrackItem item : trackItems) {
                adapter.addItem(item);
            }
            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(getScrollPosition());
            adapter.updateNowPlaying(playQueueManager.getCurrentPlayQueueItem().getUrn());
        }

    }

    private int getScrollPosition() {
        int currentPlayQueuePosition = playQueueManager.getPositionOfCurrentPlayQueueItem();
        if (currentPlayQueuePosition < 1) {
            return 0;
        } else {
            return currentPlayQueuePosition - 1;
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        subscriptions.unsubscribe();
        updateSubscription.unsubscribe();
        ButterKnife.unbind(this);
        super.onDestroyView(fragment);
    }

    @Override
    public void trackItemClicked(Urn urn) {
        playQueueManager.setCurrentPlayQueueItem(urn);
    }

}

