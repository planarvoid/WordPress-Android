package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.playback.PlayQueueManager.RepeatMode.REPEAT_ONE;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.playqueue.PlayQueueItemAnimator.Mode;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.view.adapters.PlayingTrackAware;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

class PlayQueuePresenter extends SupportFragmentLightCycleDispatcher<Fragment>
        implements TrackItemRenderer.Listener {

    private final PlayQueueRecyclerItemAdapter adapter;
    private final PlayQueueManager playQueueManager;
    private final PlayQueueOperations playQueueOperations;
    private final PlayQueueArtworkController artworkController;

    private final SwipeTrackToRemoveHelper swipeToRemoveHelper;
    private final EventBus eventBus;
    private final CompositeSubscription eventSubscriptions = new CompositeSubscription();
    private Subscription updateSubscription = RxUtils.invalidSubscription();

    @Bind(R.id.recycler_view) RecyclerView recyclerView;
    @Bind(R.id.play_queue_drawer) View playQueueDrawer;
    private PlayQueueItemAnimator animator;

    @Inject
    public PlayQueuePresenter(PlayQueueRecyclerItemAdapter adapter,
                              PlayQueueManager playQueueManager,
                              PlayQueueOperations playQueueOperations,
                              PlayQueueArtworkController playerArtworkController,
                              SwipeTrackToRemoveHelper swipeToRemoveHelper,
                              EventBus eventBus) {
        this.adapter = adapter;
        this.playQueueManager = playQueueManager;
        this.playQueueOperations = playQueueOperations;
        this.artworkController = playerArtworkController;
        this.swipeToRemoveHelper = swipeToRemoveHelper;
        this.eventBus = eventBus;
    }

    @Override
    public void onViewCreated(final Fragment fragment, final View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.bind(this, view);
        initRecyclerView(view);

        playQueueDrawer.setVisibility(View.VISIBLE);
        artworkController.bind(ButterKnife.<PlayerTrackArtworkView>findById(view, R.id.artwork_view));
        subscribeToEvents();
        refreshPlayQueue();
    }

    private void initRecyclerView(View view) {
        animator = new PlayQueueItemAnimator();
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(animator);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToRemoveCallback(swipeToRemoveHelper));
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void subscribeToEvents() {
        eventSubscriptions.add(eventBus.subscribeImmediate(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                                                           new UpdateCurrentTrackSubscriber(adapter)));
        eventSubscriptions.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new ChangePlayQueueSubscriber()));
        eventSubscriptions.add(eventBus.queue(EventQueue.PLAYBACK_PROGRESS)
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribe(new PlaybackProgressSubscriber()));
        eventSubscriptions.add(
                eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new PlaybackStateSubscriber()));
    }

    @Override
    public void onResume(Fragment fragment) {
        final View view = fragment.getView();
        setupRepeatButton(view);
        setupShuffleButton(view);

        super.onResume(fragment);
    }

    @OnClick(R.id.close_play_queue)
    public void closePlayQueue() {
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createHideEvent());
    }

    private void refreshPlayQueue() {
        updateSubscription = playQueueOperations.getTrackItems()
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(new PlayQueueSubscriber());
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
        eventSubscriptions.unsubscribe();
        updateSubscription.unsubscribe();
        ButterKnife.unbind(this);
        super.onDestroyView(fragment);
    }

    @Override
    public void trackItemClicked(Urn urn, int position) {
        playQueueManager.setCurrentPlayQueueItem(urn, position);
        adapter.updateNowPlaying(position);
    }

    private void setupRepeatButton(View view) {
        ImageView button = ButterKnife.findById(view, R.id.repeat_button);
        setupRepeatButtonIcon(button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animator.setMode(Mode.REPEAT);
                playQueueManager.setRepeatMode(getNextRepeatMode());
                updateRepeatAdapter();
                setupRepeatButtonIcon((ImageView) v);
            }
        });
    }

    private void setupShuffleButton(View view) {
        ToggleButton button = ButterKnife.findById(view, R.id.shuffle_button);
        button.setChecked(playQueueManager.isShuffled());
    }

    @OnClick(R.id.shuffle_button)
    void shuffleClicked(ToggleButton toggle) {
        if (toggle.isChecked()) {
            animator.setMode(Mode.SHUFFLING);
            playQueueManager.shuffle();
        } else {
            animator.setMode(Mode.DEFAULT);
            playQueueManager.unshuffle();
        }
    }

    private RepeatMode getNextRepeatMode() {
        final RepeatMode[] repeatModes = RepeatMode.values();
        final int currentOrdinal = playQueueManager.getRepeatMode().ordinal();
        final int nextOrdinal = (currentOrdinal + 1) % repeatModes.length;
        return repeatModes[nextOrdinal];
    }

    private void setupRepeatButtonIcon(ImageView button) {
        switch (playQueueManager.getRepeatMode()) {
            case REPEAT_ONE:
                button.setImageResource(R.drawable.ic_repeat_one);
                break;
            case REPEAT_ALL:
                button.setImageResource(R.drawable.ic_repeat_all);
                break;
            case REPEAT_NONE:
            default:
                button.setImageResource(R.drawable.ic_repeat);
        }
    }

    private void updateRepeatAdapter() {
        adapter.updateInRepeatMode(playQueueManager.getRepeatMode() == REPEAT_ONE);
    }

    private class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {

        @Override
        public void onNext(PlaybackProgressEvent progressEvent) {
            artworkController.setProgress(progressEvent.getPlaybackProgress());
        }
    }

    private class PlaybackStateSubscriber extends DefaultSubscriber<PlayStateEvent> {

        @Override
        public void onNext(PlayStateEvent stateEvent) {
            artworkController.setPlayState(stateEvent);
        }
    }

    private class UpdateCurrentTrackSubscriber extends UpdatePlayingTrackSubscriber {

        UpdateCurrentTrackSubscriber(PlayingTrackAware adapter) {
            super(adapter);
        }

        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            super.onNext(event);
            artworkController.loadArtwork(event.getCurrentPlayQueueItem().getUrnOrNotSet());
        }
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<List<TrackItem>> {
        @Override
        public void onNext(List<TrackItem> trackItems) {
            adapter.clear();
            for (TrackItem item : trackItems) {
                item.setInRepeatMode(playQueueManager.getRepeatMode() == REPEAT_ONE);
                adapter.addItem(item);
            }
            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(getScrollPosition());
            adapter.updateNowPlaying(playQueueManager.getCurrentTrackPosition());
        }
    }

    private class ChangePlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent playQueueEvent) {
            updateSubscription.unsubscribe();
            refreshPlayQueue();
        }
    }

    static class SwipeToRemoveCallback extends ItemTouchHelper.SimpleCallback {

        private final SwipeTrackToRemoveHelper swipeToRemoveHelper;

        public SwipeToRemoveCallback(SwipeTrackToRemoveHelper swipeToRemoveHelper) {
            super(0, ItemTouchHelper.RIGHT);
            this.swipeToRemoveHelper = swipeToRemoveHelper;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView,
                              RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (swipeToRemoveHelper.isRemovable(viewHolder.getAdapterPosition())) {
                return super.getMovementFlags(recyclerView, viewHolder);
            }
            return ItemTouchHelper.ACTION_STATE_IDLE;
        }


        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            swipeToRemoveHelper.remove(viewHolder.getAdapterPosition());
        }
    }

    static class SwipeTrackToRemoveHelper {
        private final PlayQueueManager playQueueManager;

        @Inject
        SwipeTrackToRemoveHelper(PlayQueueManager playQueueManager) {
            this.playQueueManager = playQueueManager;
        }

        public boolean isRemovable(int position) {
            return position > playQueueManager.getCurrentTrackPosition();
        }

        public void remove(int position) {
            playQueueManager.removeItemAtPosition(position);
        }
    }
}
