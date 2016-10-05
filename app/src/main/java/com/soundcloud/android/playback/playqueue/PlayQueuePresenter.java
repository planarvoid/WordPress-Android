package com.soundcloud.android.playback.playqueue;


import static com.soundcloud.android.playback.playqueue.TrackPlayQueueUIItem.ONLY_TRACKS;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlayableQueueItem;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.playqueue.PlayQueueItemAnimator.Mode;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PlayQueuePresenter extends SupportFragmentLightCycleDispatcher<Fragment> {

    private static final Func1<PlayQueueEvent, Boolean> isNotItemChangedEvent = new Func1<PlayQueueEvent, Boolean>() {
        @Override
        public Boolean call(PlayQueueEvent playQueueEvent) {
            return !playQueueEvent.itemMoved() && !playQueueEvent.itemRemoved() && !playQueueEvent.itemAdded();
        }
    };

    private static final Func1<CurrentPlayQueueItemEvent, Boolean> hasPositionMoved = new Func1<CurrentPlayQueueItemEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueItemEvent currentPlayQueueItemEvent) {
            return currentPlayQueueItemEvent.hasPositionChanged();
        }
    };

    private static final Func1<TrackPlayQueueUIItem, TrackAndPlayQueueItem> TO_TRACK_AND_PLAY_QUEUE_ITEM =
            new Func1<TrackPlayQueueUIItem, TrackAndPlayQueueItem>() {
                @Override
                public TrackAndPlayQueueItem call(TrackPlayQueueUIItem item) {
                    return new TrackAndPlayQueueItem(item.getTrackItem(), (TrackQueueItem) item.getPlayQueueItem());
                }
            };

    private final PlayQueueAdapter adapter;
    private final PlayQueueManager playQueueManager;
    private final PlayQueueOperations playQueueOperations;
    private final PlayQueueArtworkController artworkController;

    private final EventBus eventBus;
    private final Context context;
    private final CompositeSubscription eventSubscriptions = new CompositeSubscription();
    private final PlayQueueSwipeToRemoveCallback playQueueSwipeToRemoveCallback;
    private final FeedbackController feedbackController;

    private final PlayQueueUIItemMapper playQueueUIItemMapper;

    private Subscription updateSubscription = RxUtils.invalidSubscription();

    @Bind(R.id.recycler_view)
    RecyclerView recyclerView;
    private PlayQueueItemAnimator animator;

    @Inject
    public PlayQueuePresenter(PlayQueueAdapter adapter,
                              PlayQueueManager playQueueManager,
                              PlayQueueOperations playQueueOperations,
                              PlayQueueArtworkController playerArtworkController,
                              PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory,
                              EventBus eventBus,
                              Context context,
                              FeedbackController feedbackController,
                              PlayQueueUIItemMapper playQueueUIItemMapper) {
        this.adapter = adapter;
        this.playQueueManager = playQueueManager;
        this.playQueueOperations = playQueueOperations;
        this.artworkController = playerArtworkController;
        this.eventBus = eventBus;
        this.context = context;
        this.playQueueSwipeToRemoveCallback = swipeToRemoveCallbackFactory.create(this);
        this.feedbackController = feedbackController;
        this.playQueueUIItemMapper = playQueueUIItemMapper;
    }

    @Override
    public void onViewCreated(final Fragment fragment, final View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.bind(this, view);
        initRecyclerView();
        artworkController.bind(ButterKnife.<PlayerTrackArtworkView>findById(view, R.id.artwork_view));
        subscribeToEvents();
        loadPlayQueueUIItems();
    }

    private void initRecyclerView() {
        animator = new PlayQueueItemAnimator();
        recyclerView.setLayoutManager(new SmoothScrollLinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(animator);
        recyclerView.addItemDecoration(new TopPaddingDecorator(), 0);

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(playQueueSwipeToRemoveCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setDragListener(new DragListener() {
            @Override
            public void startDrag(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });
        adapter.setNowPlayingChangedListener(artworkController);
    }

    private void subscribeToEvents() {
        eventSubscriptions.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                       .first()
                                       .subscribe(new UpdateCurrentTrackSubscriber()));
        eventSubscriptions.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                       .filter(hasPositionMoved)
                                       .subscribe(new UpdateCurrentTrackSubscriber()));
        eventSubscriptions.add(eventBus.queue(EventQueue.PLAY_QUEUE)
                                       .filter(isNotItemChangedEvent)
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
    void closePlayQueue() {
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createHideEvent());
    }

    @OnClick(R.id.up_next)
    void scrollToNowPlaying() {
        recyclerView.smoothScrollToPosition(getScrollPosition());
    }

    private void loadPlayQueueUIItems() {
        updateSubscription = playQueueOperations.getTracks()
                                                .zipWith(playQueueOperations.getContextTitles(), playQueueUIItemMapper)
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(new PlayQueueSubscriber());
    }

    private int getScrollPosition() {
        int currentPlayQueuePosition = playQueueManager.getPositionOfCurrentPlayQueueItem();

        if (currentPlayQueuePosition > 0) {
            currentPlayQueuePosition -= 1;
        } else if (currentPlayQueuePosition < 2) {
            return 0;
        } else {
            return currentPlayQueuePosition - 2;
        }

        return adapter.getAdapterPosition(currentPlayQueuePosition);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscriptions.clear();
        updateSubscription.unsubscribe();
        ButterKnife.unbind(this);
        super.onDestroyView(fragment);
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
                button.setImageResource(R.drawable.ic_repeat_off);
        }
    }

    private void updateRepeatAdapter() {
        adapter.updateInRepeatMode(playQueueManager.getRepeatMode());
    }

    boolean isRemovable(int adapterPosition) {
        return adapter.getQueuePosition(adapterPosition) > playQueueManager.getCurrentTrackPosition();
    }

    public void remove(int adapterPosition) {
        final PlayQueueUIItem item = adapter.getItem(adapterPosition);

        if (item.isTrack()) {
            final PlayQueueItem playQueueItem = ((TrackPlayQueueUIItem) item).getPlayQueueItem();
            int removalPosition = playQueueManager.getItemPosition(playQueueItem);
            adapter.removeItem(adapterPosition);
            playQueueManager.removeItem(playQueueItem);
            Feedback feedback = Feedback.create(R.string.track_removed,
                                                R.string.undo,
                                                new UndoOnClickListener(playQueueItem,
                                                                        removalPosition,
                                                                        item,
                                                                        adapterPosition));
            feedbackController.showFeedback(feedback);
            rebuildLabels();
        }
    }

    void switchItems(int fromPosition, int toPosition) {
        adapter.switchItems(fromPosition, toPosition);
    }

    void moveItems(int fromAdapterPosition, int toAdapterPosition) {
        playQueueManager.moveItem(adapter.getQueuePosition(fromAdapterPosition),
                                  adapter.getQueuePosition(toAdapterPosition));
        rebuildLabels();
    }

    private void rebuildLabels() {
        final List<PlayQueueUIItem> uiItems = adapter.getItems();
        final Map<Urn, String> existingTitles = buildTitlesMap(uiItems);

        Observable.from(uiItems)
                  .filter(ONLY_TRACKS)
                  .cast(TrackPlayQueueUIItem.class)
                  .map(TO_TRACK_AND_PLAY_QUEUE_ITEM)
                  .toList()
                  .zipWith(Observable.just(existingTitles), playQueueUIItemMapper)
                  .subscribe(new RebuildSubscriber());
    }

    @NonNull
    private Map<Urn, String> buildTitlesMap(List<PlayQueueUIItem> items) {
        final Map<Urn, String> titles = new HashMap<>();

        for (PlayQueueUIItem item : items) {
            if (item.isTrack()) {
                final TrackPlayQueueUIItem trackUIItem = (TrackPlayQueueUIItem) item;
                final Optional<String> contextTitle = trackUIItem.getContextTitle();
                if (contextTitle.isPresent()) {
                    final PlayableQueueItem playQueueItem = (PlayableQueueItem) trackUIItem.getPlayQueueItem();
                    final Optional<Urn> urn = playQueueItem.getPlaybackContext().urn();
                    if (urn.isPresent()) {
                        titles.put(urn.get(), contextTitle.get());
                    }
                }
            }
        }
        return titles;
    }

    private void rebuildAdapter(List<PlayQueueUIItem> items) {
        adapter.clear();

        for (PlayQueueUIItem item : items) {
            adapter.addItem(item);
        }

        adapter.notifyDataSetChanged();
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
            adapter.updateNowPlaying(adapter.getAdapterPosition(event.getPosition()), true);
        }
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {
        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            rebuildAdapter(items);
            recyclerView.scrollToPosition(getScrollPosition());
            adapter.updateNowPlaying(adapter.getAdapterPosition(playQueueManager.getCurrentTrackPosition()), true);
        }
    }

    private class RebuildSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {
        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            rebuildAdapter(items);
            adapter.updateNowPlaying(adapter.getAdapterPosition(playQueueManager.getCurrentTrackPosition()), false);
        }
    }


    private class ChangePlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent playQueueEvent) {
            updateSubscription.unsubscribe();
            loadPlayQueueUIItems();
        }
    }

    interface DragListener {

        void startDrag(RecyclerView.ViewHolder viewHolder);

    }

    private static class TopPaddingDecorator extends RecyclerView.ItemDecoration {

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == 0) {
                outRect.top = ViewUtils.dpToPx(view.getContext(), 72);
                outRect.left = 0;
                outRect.right = 0;
                outRect.bottom = 0;
            }
        }
    }

    private class UndoOnClickListener implements View.OnClickListener {

        private final PlayQueueItem playQueueItem;
        private final int playQueuePosition;
        private final PlayQueueUIItem playQueueUIItem;
        private final int adapterPosition;

        public UndoOnClickListener(PlayQueueItem playQueueItem,
                                   int playQueuePosition,
                                   PlayQueueUIItem playQueueUIItem,
                                   int adapterPosition) {
            this.playQueueItem = playQueueItem;
            this.playQueuePosition = playQueuePosition;
            this.playQueueUIItem = playQueueUIItem;
            this.adapterPosition = adapterPosition;
        }

        @Override
        public void onClick(View view) {
            playQueueManager.insertItemAtPosition(playQueuePosition, playQueueItem);
            adapter.addItem(adapterPosition, playQueueUIItem);
            rebuildLabels();
        }
    }

}
