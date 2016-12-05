package com.soundcloud.android.playback.playqueue;


import static com.soundcloud.android.playback.playqueue.TrackPlayQueueUIItem.ONLY_TRACKS;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlayableQueueItem;
import com.soundcloud.android.playback.TrackQueueItem;
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
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
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

    private static final Func1<TrackPlayQueueUIItem, TrackAndPlayQueueItem> TO_TRACK_AND_PLAY_QUEUE_ITEM = item ->
            new TrackAndPlayQueueItem(item.getTrackItem(), (TrackQueueItem) item.getPlayQueueItem());

    private final PlayQueueAdapter adapter;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionController playSessionController;
    private final PlayQueueOperations playQueueOperations;
    private final PlayQueueArtworkController artworkController;

    private final EventBus eventBus;
    private final Context context;
    private final CompositeSubscription eventSubscriptions = new CompositeSubscription();
    private final PlayQueueSwipeToRemoveCallback playQueueSwipeToRemoveCallback;
    private final FeedbackController feedbackController;
    private final PlayQueueUIItemMapper playQueueUIItemMapper;

    private Subscription updateSubscription = RxUtils.invalidSubscription();

    @Bind(R.id.recycler_view) RecyclerView recyclerView;
    private Observable<List<TrackAndPlayQueueItem>> cachedTracks = Observable.empty();
    private Observable<Map<Urn, String>> cachedTitles = Observable.empty();

    @Inject
    PlayQueuePresenter(PlayQueueAdapter adapter,
                       PlayQueueManager playQueueManager,
                       PlaySessionController playSessionController,
                       PlayQueueOperations playQueueOperations,
                       PlayQueueArtworkController playerArtworkController,
                       PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory,
                       EventBus eventBus,
                       Context context,
                       FeedbackController feedbackController,
                       PlayQueueUIItemMapper playQueueUIItemMapper) {
        this.adapter = adapter;
        this.playQueueManager = playQueueManager;
        this.playSessionController = playSessionController;
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
        artworkController.bind(ButterKnife.findById(view, R.id.artwork_view));
        subscribeToEvents();
        loadPlayQueueUIItems();
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new SmoothScrollLinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new TopPaddingDecorator(), 0);
        recyclerView.setItemAnimator(buildItemAnimator());

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(playQueueSwipeToRemoveCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setDragListener(itemTouchHelper::startDrag);
        adapter.setNowPlayingChangedListener(artworkController);
        adapter.setTrackClickListener(new TrackClickListener());
    }

    private DefaultItemAnimator buildItemAnimator() {
        final DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setRemoveDuration(150);
        return animator;
    }

    @VisibleForTesting
    void subscribeToEvents() {
        setAdapterStreams();
        setArtworkStreams();
    }

    private void setAdapterStreams() {
        eventSubscriptions.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                       .filter(currentPlayQueueItemEvent -> !isPlayingCurrent())
                                       .flatMap(event -> fetchPlayQueueUIItems())
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribe(new UpdateNowPlayingSubscriber()));
        eventSubscriptions.add(eventBus.queue(EventQueue.PLAY_QUEUE)
                                       .filter(playQueueEvent -> !playQueueEvent.itemChanged())
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribe(new ChangePlayQueueSubscriber()));
    }

    private void setArtworkStreams() {
        eventSubscriptions.add(eventBus.queue(EventQueue.PLAYBACK_PROGRESS)
                                       .map(PlaybackProgressEvent::getPlaybackProgress)
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribe(artworkController::setProgress));
        eventSubscriptions.add(eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribe(artworkController::setPlayState));
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
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueClose());
    }

    @OnClick(R.id.up_next)
    void scrollToNowPlaying() {
        recyclerView.smoothScrollToPosition(getScrollPosition());
    }

    private void loadPlayQueueUIItems() {
        setCachedObservables();
        updateSubscription = fetchPlayQueueUIItems().subscribe(new PlayQueueSubscriber());
    }

    @VisibleForTesting
    void setCachedObservables() {
        cachedTracks = playQueueOperations.getTracks().cache();
        cachedTitles = playQueueOperations.getContextTitles().cache();
    }

    private Observable<List<PlayQueueUIItem>> fetchPlayQueueUIItems() {
        return cachedTracks
                .zipWith(cachedTitles, playQueueUIItemMapper)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private int getScrollPosition() {
        int currentPlayQueuePosition = adapter.getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());

        if (currentPlayQueuePosition > 0) {
            currentPlayQueuePosition -= 1;
        } else if (currentPlayQueuePosition < 2) {
            return 0;
        } else {
            return currentPlayQueuePosition - 2;
        }

        return currentPlayQueuePosition;
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscriptions.clear();
        updateSubscription.unsubscribe();
        ButterKnife.unbind(this);
        super.onDestroyView(fragment);
    }

    private void setupRepeatButton(View view) {
        final ImageView button = ButterKnife.findById(view, R.id.repeat_button);
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

    private void setupShuffleButton(View view) {
        ToggleButton button = ButterKnife.findById(view, R.id.shuffle_button);
        button.setChecked(playQueueManager.isShuffled());
    }

    @OnClick(R.id.repeat_button)
    void repeatClicked(ImageView view) {
        final RepeatMode nextRepeatMode = getNextRepeatMode();

        playQueueManager.setRepeatMode(nextRepeatMode);
        adapter.updateInRepeatMode(nextRepeatMode);

        switch (nextRepeatMode) {
            case REPEAT_ONE:
                view.setImageResource(R.drawable.ic_repeat_one);
                break;
            case REPEAT_ALL:
                view.setImageResource(R.drawable.ic_repeat_all);
                break;
            case REPEAT_NONE:
            default:
                view.setImageResource(R.drawable.ic_repeat_off);
        }

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRepeat(Screen.PLAY_QUEUE, nextRepeatMode));
    }

    @OnClick(R.id.shuffle_button)
    void shuffleClicked(ToggleButton toggle) {
        final boolean isShuffled = toggle.isChecked();

        if (isShuffled) {
            playQueueManager.shuffle();
        } else {
            playQueueManager.unshuffle();
        }

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueShuffle(isShuffled));
    }

    private RepeatMode getNextRepeatMode() {
        final RepeatMode[] repeatModes = RepeatMode.values();
        final int currentOrdinal = playQueueManager.getRepeatMode().ordinal();
        return repeatModes[(currentOrdinal + 1) % repeatModes.length];
    }

    boolean isRemovable(int adapterPosition) {
        if (adapterPosition >= 0 && adapterPosition < adapter.getItemCount()) {
            final PlayQueueUIItem item = adapter.getItem(adapterPosition);
            return item.isTrack() && PlayState.COMING_UP.equals(item.getPlayState());
        } else {
            return false;
        }
    }

    public void remove(int adapterPosition) {
        final PlayQueueUIItem adapterItem = adapter.getItem(adapterPosition);

        if (adapterItem.isTrack()) {
            final PlayQueueItem playQueueItem = ((TrackPlayQueueUIItem) adapterItem).getPlayQueueItem();
            final int playQueuePosition = playQueueManager.indexOfPlayQueueItem(playQueueItem);

            adapter.removeItem(adapterPosition);

            if (playQueuePosition >= 0) {
                playQueueManager.removeItem(playQueueItem);
                showFeedback(adapterPosition, adapterItem, playQueueItem, playQueuePosition);
                setCachedObservables();
            }

            rebuildLabels();
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRemove(Screen.PLAY_QUEUE));
        }
    }

    private void showFeedback(int adapterPosition,
                              PlayQueueUIItem adapterItem,
                              PlayQueueItem playQueueItem,
                              int playQueuePosition) {
        final Feedback feedback = Feedback.create(R.string.track_removed, R.string.undo,
                                                  new UndoOnClickListener(playQueueItem,
                                                                          playQueuePosition,
                                                                          adapterItem,
                                                                          adapterPosition));
        feedbackController.showFeedback(feedback);
    }

    void switchItems(int fromPosition, int toPosition) {
        adapter.switchItems(fromPosition, toPosition);
    }

    void moveItems(int fromAdapterPosition, int toAdapterPosition) {
        playQueueManager.moveItem(adapter.getQueuePosition(fromAdapterPosition),
                                  adapter.getQueuePosition(toAdapterPosition));
        rebuildLabels();
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueReorder(Screen.PLAY_QUEUE));
    }

    private void rebuildLabels() {
        rebuildPlayQueueUIItemsObservable(adapter.getItems())
                .subscribe(new RebuildSubscriber());
    }

    @VisibleForTesting
    Observable<List<PlayQueueUIItem>> rebuildPlayQueueUIItemsObservable(List<PlayQueueUIItem> uiItems) {
        final Map<Urn, String> existingTitles = buildTitlesMap(uiItems);
        return Observable.from(uiItems)
                         .filter(ONLY_TRACKS)
                         .cast(TrackPlayQueueUIItem.class)
                         .map(TO_TRACK_AND_PLAY_QUEUE_ITEM)
                         .toList()
                         .zipWith(Observable.just(existingTitles), playQueueUIItemMapper);
    }

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

    private boolean isPlayingCurrent() {
        final int adapterPosition = adapter.getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());

        if (adapterPosition < adapter.getItems().size() && adapterPosition >= 0) {
            return adapter.getItem(adapterPosition).getPlayState().equals(PlayState.PLAYING);
        }

        return false;
    }

    private class UpdateNowPlayingSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {

        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            if (items.size() != adapter.getItemCount()) {
                rebuildAdapter(items);
            }
            adapter.updateNowPlaying(adapter.getAdapterPosition(playQueueManager.getCurrentPlayQueueItem()), true);
        }
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {

        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            boolean wasEmpty = adapter.isEmpty();

            rebuildAdapter(items);
            if (wasEmpty) {
                recyclerView.scrollToPosition(getScrollPosition());
            }
            adapter.updateNowPlaying(adapter.getAdapterPosition(playQueueManager.getCurrentPlayQueueItem()), true);
        }

    }

    private class RebuildSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {

        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            rebuildAdapter(items);
            adapter.updateNowPlaying(adapter.getAdapterPosition(playQueueManager.getCurrentPlayQueueItem()), false);
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

    private class TrackClickListener implements TrackPlayQueueItemRenderer.TrackClickListener {

        @Override
        public void trackClicked(final int listPosition) {
            if (adapter.getItem(listPosition).isTrack()) {
                adapter.updateNowPlaying(listPosition, true);
                playQueueManager.setCurrentPlayQueueItem(((TrackPlayQueueUIItem) adapter.getItem(listPosition)).getPlayQueueItem());

                if (!playSessionController.isPlayingCurrentPlayQueueItem()) {
                    playSessionController.play();
                }
            }
        }
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

        UndoOnClickListener(PlayQueueItem playQueueItem,
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
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRemoveUndo(Screen.PLAY_QUEUE));
        }
    }

}
