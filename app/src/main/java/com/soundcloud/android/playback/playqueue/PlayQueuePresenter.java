package com.soundcloud.android.playback.playqueue;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.playqueue.PlayQueueItemAnimator.Mode;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;

import java.util.List;

import javax.inject.Inject;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;


import java.util.ArrayList;

import static com.soundcloud.android.playback.PlayQueueManager.RepeatMode.REPEAT_ONE;

class PlayQueuePresenter extends SupportFragmentLightCycleDispatcher<Fragment>
        implements TrackItemRenderer.Listener {

    private final Func1<PlayQueueEvent, Boolean> isNotMoveOrRemovedEvent = new Func1<PlayQueueEvent, Boolean>() {
        @Override
        public Boolean call(PlayQueueEvent playQueueEvent) {
            return !playQueueEvent.itemMoved() && !playQueueEvent.itemRemoved();
        }
    };

    private final PlayQueueRecyclerItemAdapter adapter;
    private final PlayQueueManager playQueueManager;
    private final PlayQueueOperations playQueueOperations;
    private final PlayQueueArtworkController artworkController;

    private final EventBus eventBus;
    private final Context context;
    private final CompositeSubscription eventSubscriptions = new CompositeSubscription();
    private final PlayQueueSwipeToRemoveCallback playQueueSwipeToRemoveCallback;
    private final Func1<List<TrackAndPlayQueueItem>, List<PlayQueueUIItem>> toPlayQueueUIItem = new Func1<List<TrackAndPlayQueueItem>, List<PlayQueueUIItem>>() {
        @Override
        public List<PlayQueueUIItem> call(List<TrackAndPlayQueueItem> trackAndPlayQueueItems) {
            final List<PlayQueueUIItem> items = new ArrayList<>();
            for (TrackAndPlayQueueItem trackAndPlayQueueItem : trackAndPlayQueueItems) {
                items.add(PlayQueueUIItem.from(trackAndPlayQueueItem.playQueueItem, trackAndPlayQueueItem.trackItem, context));
            }
            return items;
        }
    };
    private Subscription updateSubscription = RxUtils.invalidSubscription();

    @Bind(R.id.recycler_view)
    RecyclerView recyclerView;
    @Bind(R.id.play_queue_drawer)
    View playQueueDrawer;
    private PlayQueueItemAnimator animator;

    @Inject
    public PlayQueuePresenter(PlayQueueRecyclerItemAdapter adapter,
                              PlayQueueManager playQueueManager,
                              PlayQueueOperations playQueueOperations,
                              PlayQueueArtworkController playerArtworkController,
                              PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory,
                              EventBus eventBus,
                              Context context) {
        this.adapter = adapter;
        this.playQueueManager = playQueueManager;
        this.playQueueOperations = playQueueOperations;
        this.artworkController = playerArtworkController;
        this.eventBus = eventBus;
        this.context = context;
        this.playQueueSwipeToRemoveCallback = swipeToRemoveCallbackFactory.create(this);
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

    private void initRecyclerView(final View view) {
        animator = new PlayQueueItemAnimator();
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(animator);

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(playQueueSwipeToRemoveCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setDragListener(new DragListener() {
            @Override
            public void startDrag(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });
    }

    private void subscribeToEvents() {
        eventSubscriptions.add(eventBus.subscribeImmediate(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                new UpdateCurrentTrackSubscriber()));
        eventSubscriptions.add(eventBus.queue(EventQueue.PLAY_QUEUE)
                .filter(isNotMoveOrRemovedEvent)
                .subscribe(new ChangePlayQueueSubscriber()));
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
        updateSubscription = playQueueOperations.getTracks()
                .map(toPlayQueueUIItem)
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

    public boolean isRemovable(int position) {
        return position > playQueueManager.getCurrentTrackPosition();
    }

    public void remove(int position) {
        final PlayQueueItem playQueueItem = adapter.getItem(position).getPlayQueueItem();
        adapter.removeItem(position);
        playQueueManager.removeItem(playQueueItem);
    }

    public void switchItems(int fromPosition, int toPosition) {
        adapter.switchItems(fromPosition, toPosition);
    }

    public void moveItems(int fromPosition, int toPosition) {
        playQueueManager.moveItem(fromPosition, toPosition);
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

    private class UpdateCurrentTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {


        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            artworkController.loadArtwork(event.getCurrentPlayQueueItem().getUrnOrNotSet());
            adapter.updateNowPlaying(event.getPosition());
            adapter.notifyDataSetChanged();
        }
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {
        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            adapter.clear();
            for (PlayQueueUIItem item : items) {
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

    public interface DragListener {

        void startDrag(RecyclerView.ViewHolder viewHolder);

    }

}
