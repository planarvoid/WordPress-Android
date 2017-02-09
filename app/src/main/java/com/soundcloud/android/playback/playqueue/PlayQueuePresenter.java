package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlayableQueueItem;
import com.soundcloud.android.playback.PlaybackStateProvider;
import com.soundcloud.android.playback.PlaylistExploder;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PlayQueuePresenter {

    private static final int EXPLOSION_LOOK_AHEAD = 5;

    private final PlayQueueManager playQueueManager;
    private final PlaybackStateProvider playbackStateProvider;
    private final PlaySessionController playSessionController;
    private final PlaylistExploder playlistExploder;
    private final PlayQueueDataProvider playQueueDataProvider;
    private final EventBus eventBus;
    private final PlayQueueUIItemMapper playQueueUIItemMapper;
    private final CompositeSubscription eventSubscriptions = new CompositeSubscription();
    private final Subject<Boolean, Boolean> rebuildSubject = PublishSubject.create();

    private Optional<PlayQueueView> playQueueView = Optional.absent();
    private Optional<UndoHolder> undoHolder = Optional.absent();
    private List<PlayQueueUIItem> items = new ArrayList<>();
    private boolean resetUI = true;

    @Inject
    PlayQueuePresenter(PlayQueueManager playQueueManager,
                       PlaybackStateProvider playbackStateProvider,
                       PlaySessionController playSessionController,
                       PlayQueueDataProvider dataProvider,
                       PlaylistExploder playlistExploder,
                       EventBus eventBus,
                       PlayQueueUIItemMapper playQueueUIItemMapper) {
        this.playQueueManager = playQueueManager;
        this.playbackStateProvider = playbackStateProvider;
        this.playSessionController = playSessionController;
        this.playQueueDataProvider = dataProvider;
        this.playlistExploder = playlistExploder;
        this.eventBus = eventBus;
        this.playQueueUIItemMapper = playQueueUIItemMapper;
    }

    void attachView(PlayQueueView playQueueView) {
        this.playQueueView = Optional.of(playQueueView);
        if (this.playQueueView.isPresent()) {
            setRepeatMode(playQueueManager.getRepeatMode());
            this.playQueueView.get().setShuffledState(playQueueManager.isShuffled());

            if (items.isEmpty()) {
                this.playQueueView.get().showLoadingIndicator();
            }
            playQueueDataProvider.getPlayQueueUIItems()
                                 .observeOn(AndroidSchedulers.mainThread())
                                 .doOnNext(newItems -> items = newItems)
                                 .subscribe(new PlayQueueSubscriber());
            setUpPlaybackStream();
            setUpRebuildStream();
        }
    }

    void detachContract() {
        playQueueView = Optional.absent();
        eventSubscriptions.clear();
        resetUI = true;
    }

    private void setUpPlaybackStream() {
        eventSubscriptions.add(eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                                       .skip(1)
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .map(event -> items)
                                       .subscribe(new PlayQueueSubscriber()));
    }

    private void setUpRebuildStream() {
       eventSubscriptions.add(rebuildSubject.map(ignored -> items)
                      .map(items -> Iterables.filter(items, input -> input.isTrack()))
                      .map(trackItems -> Lists.newArrayList(Iterables.transform(trackItems, input -> {
                          TrackPlayQueueUIItem item = (TrackPlayQueueUIItem) input;
                          return new TrackAndPlayQueueItem(item.getTrackItem(), (TrackQueueItem) item.getPlayQueueItem());
                      })))
                      .zipWith(getTitlesObservable(), playQueueUIItemMapper)
                      .doOnNext(newItems -> items = newItems)
                      .doOnNext(items -> setNowPlaying())
                      .subscribe(new PlayQueueSubscriber()));
    }


    public void undoClicked() {
        if (playQueueView.isPresent() && undoHolder.isPresent()) {
            UndoHolder undoHolder = this.undoHolder.get();
            items.addAll(undoHolder.adapterPosition, undoHolder.playQueueUIItem);
            rebuildSubject.onNext(true);
            playQueueManager.insertItemAtPosition(undoHolder.playQueuePosition, undoHolder.playQueueItem);

            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRemoveUndo(Screen.PLAY_QUEUE));
        }
    }

    void closePlayQueue() {
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createHideEvent());
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueClose());
    }

    void onNextClick() {
        if (playQueueView.isPresent()) {
            playQueueView.get().scrollTo(getScrollPosition());
        }
    }

    public void trackClicked(int listPosition) {
        if (items.get(listPosition).isTrack()) {
            updateNowPlaying(listPosition, playbackStateProvider.isSupposedToBePlaying());
            playQueueView.get().setItems(items);
            TrackPlayQueueUIItem trackItem = (TrackPlayQueueUIItem) items.get(listPosition);
            playQueueManager.setCurrentPlayQueueItem(trackItem.getPlayQueueItem());
            if (trackItem.isGoTrack()) {
                playQueueView.get().setGoPlayerStrip();
            } else {
                playQueueView.get().setDefaultPlayerStrip();
            }

            if (playSessionController.isPlayingCurrentPlayQueueItem()) {
                playSessionController.togglePlayback();
            } else {
                playSessionController.play();
            }
        }
    }

    private int getScrollPosition() {

        int currentPlayQueuePosition = getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());

        if (currentPlayQueuePosition > 0) {
            currentPlayQueuePosition -= 1;
        } else if (currentPlayQueuePosition < 2) {
            return 0;
        } else {
            return currentPlayQueuePosition - 2;
        }

        return currentPlayQueuePosition;
    }

    void repeatClicked() {
        final RepeatMode nextRepeatMode = getNextRepeatMode();
        playQueueManager.setRepeatMode(nextRepeatMode);
        setRepeatMode(nextRepeatMode);
        updateItemsInRepeatMode(nextRepeatMode);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRepeat(Screen.PLAY_QUEUE, nextRepeatMode));
    }

    private void setRepeatMode(RepeatMode nextRepeatMode) {
        if (playQueueView.isPresent()) {
            switch (nextRepeatMode) {
                case REPEAT_ONE:
                    playQueueView.get().setRepeatOne();
                    break;
                case REPEAT_ALL:
                    playQueueView.get().setRepeatAll();
                    break;
                case REPEAT_NONE:
                default:
                    playQueueView.get().setRepeatNone();
            }
        }
    }

    private void updateItemsInRepeatMode(PlayQueueManager.RepeatMode repeatMode) {
        for (PlayQueueUIItem item : items) {
            item.setRepeatMode(repeatMode);
        }
        if (playQueueView.isPresent()) {
            playQueueView.get().setItems(items);
        }
    }

    void shuffleClicked(boolean isShuffled) {
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
        if (playQueueView.isPresent()) {
            if (adapterPosition >= 0 && adapterPosition < items.size()) {
                final PlayQueueUIItem item = items.get(adapterPosition);
                return item.isTrack() && PlayState.COMING_UP.equals(item.getPlayState());
            }
        }
        return false;
    }

    public void remove(int adapterPosition) {
        if (playQueueView.isPresent()) {
            final PlayQueueUIItem adapterItem = items.get(adapterPosition);
            if (adapterItem.isTrack()) {
                playQueueView.get().showUndo();
                final PlayQueueItem playQueueItem = ((TrackPlayQueueUIItem) adapterItem).getPlayQueueItem();
                final int playQueuePosition = playQueueManager.indexOfPlayQueueItem(playQueueItem);
                if (isHeader(adapterPosition-1) && isHeader(adapterPosition+1)) {
                    playQueueView.get().removeItem(adapterPosition);
                    playQueueView.get().removeItem(adapterPosition);
                    playQueueView.get().removeItem(adapterPosition-1);
                    List<PlayQueueUIItem> playQueueUIItems = new ArrayList<>(3);
                    playQueueUIItems.add(items.remove(adapterPosition));
                    playQueueUIItems.add(items.remove(adapterPosition));
                    playQueueUIItems.add(items.remove(adapterPosition-1));
                    undoHolder = Optional.of(new UndoHolder(playQueueItem, playQueuePosition, playQueueUIItems, adapterPosition));
                } else {
                    playQueueView.get().removeItem(adapterPosition);
                    undoHolder = Optional.of(new UndoHolder(playQueueItem, playQueuePosition, Lists.newArrayList(items.remove(adapterPosition)), adapterPosition));
                }

                if (playQueuePosition >= 0) {
                    playQueueManager.removeItem(playQueueItem);
                }
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRemove(Screen.PLAY_QUEUE));
            }
        }
    }

    private boolean isHeader(int adapterPosition) {
        return items.size() >= adapterPosition && adapterPosition >= 0 && items.get(adapterPosition).isHeader();
    }

    void switchItems(int fromPosition, int toPosition) {
        if (playQueueView.isPresent()) {
            Collections.swap(items, fromPosition, toPosition);
            playQueueView.get().switchItems(fromPosition, toPosition);
        }
    }

    void magicBoxClicked() {
        playQueueManager.moveToNextRecommendationItem();
    }

    void magicBoxToggled(boolean checked) {
        playQueueManager.setAutoPlay(checked);
    }

    void moveItems(int fromAdapterPosition, int toAdapterPosition) {
        if (playQueueView.isPresent()) {
            rebuildSubject.onNext(true);
            playQueueManager.moveItem(getQueuePosition(fromAdapterPosition),
                                      getQueuePosition(toAdapterPosition));
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueReorder(Screen.PLAY_QUEUE));
        }
    }

    void scrollDown(int lastVisibleItemPosition) {
        playlistExploder.explodePlaylists(lastVisibleItemPosition, EXPLOSION_LOOK_AHEAD);
    }

    void scrollUp(int firstVisibleItemPosition) {
        int resolvedPosition = (firstVisibleItemPosition - EXPLOSION_LOOK_AHEAD < 0) ? 0 : firstVisibleItemPosition - EXPLOSION_LOOK_AHEAD;
        playlistExploder.explodePlaylists(resolvedPosition, EXPLOSION_LOOK_AHEAD);
    }

    private int getQueuePosition(int adapterPosition) {
        int cursor = 0;

        for (int i = 0; i < items.size(); i++) {
            if (i == adapterPosition) {
                return cursor;
            }

            if (items.get(i).isTrack()) {
                cursor++;
            }
        }
        return 0;
    }

    private Observable<Map<Urn, String>> getTitlesObservable() {
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
        return Observable.just(titles);
    }

    private boolean isPlayingCurrent() {
        final int adapterPosition = getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());
        final boolean isWithinRange = adapterPosition < items.size() && adapterPosition >= 0;
        return isWithinRange && items.get(adapterPosition).isPlayingOrPaused();
    }

    private int getAdapterPosition(final PlayQueueItem currentPlayQueueItem) {
        return Iterables.indexOf(items, getPositionForItemPredicate(currentPlayQueueItem));
    }

    private static Predicate<PlayQueueUIItem> getPositionForItemPredicate(final PlayQueueItem currentPlayQueueItem) {
        return input -> input.isTrack() &&
                ((TrackPlayQueueUIItem) input).getPlayQueueItem().equals(currentPlayQueueItem);
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {

        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            if (playQueueView.isPresent()) {
                setNowPlaying();
                playQueueView.get().setItems(items);
                if (resetUI) {
                    playQueueView.get().scrollTo(getScrollPosition());
                    playQueueView.get().removeLoadingIndicator();
                    resetUI = false;
                }
            }
        }

    }

    private void updateNowPlaying(int position, boolean isPlaying) {
        if (items.size() == position && items.get(position).isTrack()
                && items.get(position).isPlayingOrPaused()) {
            return;
        }

        Optional<HeaderPlayQueueUIItem> lastHeaderPlayQueueUiItem = Optional.absent();
        boolean headerPlayStateSet = false;
        for (int i = 0; i < items.size(); i++) {
            final PlayQueueUIItem item = items.get(i);
            if (item.isTrack()) {
                final TrackPlayQueueUIItem trackItem = (TrackPlayQueueUIItem) item;
                setPlayState(position, i, trackItem, isPlaying);
                if (!headerPlayStateSet && lastHeaderPlayQueueUiItem.isPresent()) {
                    headerPlayStateSet = shouldAddHeader(trackItem);
                    lastHeaderPlayQueueUiItem.get().setPlayState(trackItem.getPlayState());
                }
            } else if (item.isHeader()) {
                lastHeaderPlayQueueUiItem = Optional.of((HeaderPlayQueueUIItem) item);
                headerPlayStateSet = false;
            }
        }
    }

    private void setNowPlaying() {
        final int adapterPosition = getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());
            updateNowPlaying(adapterPosition, playbackStateProvider.isSupposedToBePlaying());
    }

    private boolean shouldAddHeader(TrackPlayQueueUIItem trackItem) {
        return trackItem.isPlayingOrPaused() || PlayState.COMING_UP.equals(trackItem.getPlayState());
    }

    private void setPlayState(int currentlyPlayingPosition, int itemPosition, TrackPlayQueueUIItem item, boolean isPlaying) {
        if (currentlyPlayingPosition == itemPosition) {
            item.setPlayState(isPlaying ? PlayState.PLAYING : PlayState.PAUSED);
        } else if (itemPosition > currentlyPlayingPosition) {
            item.setPlayState(PlayState.COMING_UP);
        } else {
            item.setPlayState(PlayState.PLAYED);
        }
    }

    private class UndoHolder {
        private final PlayQueueItem playQueueItem;
        private final int playQueuePosition;
        private final List<PlayQueueUIItem> playQueueUIItem;
        private final int adapterPosition;

        public UndoHolder(PlayQueueItem playQueueItem, int playQueuePosition, List<PlayQueueUIItem> playQueueUIItem, int adapterPosition) {
            this.playQueueItem = playQueueItem;
            this.playQueuePosition = playQueuePosition;
            this.playQueueUIItem = playQueueUIItem;
            this.adapterPosition = adapterPosition;
        }

    }

}
