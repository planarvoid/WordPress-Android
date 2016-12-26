package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.events.CurrentPlayQueueItemEvent.fromNewQueue;
import static com.soundcloud.android.events.CurrentPlayQueueItemEvent.fromPositionChanged;
import static com.soundcloud.android.playback.playqueue.PlayQueueUIItem.Kind.MAGIC_BOX;
import static com.soundcloud.android.playback.playqueue.PlayState.COMING_UP;
import static com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem.createTrackWithContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateProvider;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayQueuePresenterTest extends AndroidUnitTest {

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlayQueueAdapter adapter;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlaySessionController playSessionController;
    @Mock private PlayQueueArtworkController playerArtworkController;
    @Mock private PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory;
    @Mock private PlaybackStateProvider playbackStateProvider;

    @Mock private PlayQueueUIItem item;
    @Mock private FeedbackController feedbackController;
    @Mock private View view;

    @Captor private ArgumentCaptor<Feedback> feedbackCaptor;

    private PlayQueuePresenter presenter;
    private TestEventBus eventBus = new TestEventBus();
    private final PlayQueueUIItem headerItem = headerPlayQueueUIItem();
    private final PublishSubject<List<TrackAndPlayQueueItem>> tracksSubject = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        final PlayQueueUIItemMapper playQueueUIItemMapper = new PlayQueueUIItemMapper(context(), playQueueManager);

        presenter = new PlayQueuePresenter(
                adapter,
                playQueueManager,
                playbackStateProvider,
                playSessionController,
                playQueueOperations,
                playerArtworkController,
                swipeToRemoveCallbackFactory,
                eventBus,
                context(),
                feedbackController,
                playQueueUIItemMapper
                );
        when(adapter.getItem(anyInt())).thenReturn(item);
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(item.isTrack()).thenReturn(true);
        when(playbackStateProvider.isSupposedToBePlaying()).thenReturn(true);
    }

    @Test
    public void returnTrueWhenUpcomingTrack() {
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueUIItemWithPlayState(COMING_UP);
        when(adapter.getItemCount()).thenReturn(10);
        when(adapter.getItem(2)).thenReturn(upcomingTrack);

        assertThat(presenter.isRemovable(2)).isTrue();
    }

    @Test
    public void returnFalseWhenCurrentTrack() {
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueUIItemWithPlayState(PlayState.PLAYING);
        when(adapter.getItemCount()).thenReturn(10);
        when(adapter.getItem(2)).thenReturn(upcomingTrack);

        assertThat(presenter.isRemovable(2)).isFalse();
    }

    @Test
    public void returnFalseWhenPlayedTrack() {
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueUIItemWithPlayState(PlayState.PLAYED);
        when(adapter.getItemCount()).thenReturn(10);
        when(adapter.getItem(2)).thenReturn(upcomingTrack);

        assertThat(presenter.isRemovable(2)).isFalse();
    }

    @Test
    public void shouldRemoveItemAtPosition() {
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueUIItemWithPlayState(COMING_UP);
        final PlayQueueItem playQueueItem = upcomingTrack.getPlayQueueItem();
        when(adapter.getItem(2)).thenReturn(upcomingTrack);
        when(playQueueManager.indexOfPlayQueueItem(playQueueItem)).thenReturn(1);
        when(playQueueOperations.getTracks()).thenReturn(tracksSubject);
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));

        presenter.remove(2);

        verify(adapter).removeItem(2);
        verify(playQueueManager).removeItem(playQueueItem);
    }

    @Test
    public void shouldSubscribeToCurrentPlayQueueItem() {
        setCachedObservables();

        presenter.setCachedObservables();
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueUIItemWithPlayState(COMING_UP);
        final PlayQueueItem queueItem = upcomingTrack.getPlayQueueItem();
        final CurrentPlayQueueItemEvent event = fromNewQueue(queueItem, Urn.NOT_SET, 0);
        when(adapter.getAdapterPosition(queueItem)).thenReturn(0);

        presenter.subscribeToEvents();
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);

        verify(adapter).updateNowPlaying(0, true, true);
    }

    @Test
    public void shouldSubscribeToCurrentPlayQueueItemForPositionChanged() {
        setCachedObservables();

        presenter.setCachedObservables();
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueUIItemWithPlayState(COMING_UP);
        final PlayQueueItem queueItem = upcomingTrack.getPlayQueueItem();
        final CurrentPlayQueueItemEvent event = fromPositionChanged(queueItem, Urn.NOT_SET, 0);
        when(adapter.getAdapterPosition(queueItem)).thenReturn(0);

        presenter.subscribeToEvents();
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);

        verify(adapter).updateNowPlaying(0, true, true);
    }

    @Test
    public void shouldSetPausedStateWhenNotPlayingCurrent() {
        setCachedObservables();
        when(playbackStateProvider.isSupposedToBePlaying()).thenReturn(false);

        presenter.setCachedObservables();
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueUIItemWithPlayState(COMING_UP);
        final PlayQueueItem queueItem = upcomingTrack.getPlayQueueItem();
        final CurrentPlayQueueItemEvent event = fromNewQueue(queueItem, Urn.NOT_SET, 0);
        when(adapter.getAdapterPosition(queueItem)).thenReturn(0);

        presenter.subscribeToEvents();
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);

        verify(adapter).updateNowPlaying(0, true, false);
    }

    @Test
    public void shouldSubscribeToPlayQueueChanged() {
        when(playQueueOperations.getTracks()).thenReturn(tracksSubject);
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.<Urn, String>emptyMap()));
        final PlayQueueEvent event = PlayQueueEvent.fromNewQueue(Urn.NOT_SET);

        presenter.subscribeToEvents();
        eventBus.publish(EventQueue.PLAY_QUEUE, event);

        assertThat(tracksSubject.hasObservers()).isTrue();
    }

    @Test
    public void shouldSubscribeToPlayQueueChangedAndFilterOutItemChanges() {
        when(playQueueOperations.getTracks()).thenReturn(tracksSubject);
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.<Urn, String>emptyMap()));
        final PlayQueueEvent event = PlayQueueEvent.fromQueueUpdateMoved(Urn.NOT_SET);

        presenter.subscribeToEvents();
        eventBus.publish(EventQueue.PLAY_QUEUE, event);

        assertThat(tracksSubject.hasObservers()).isFalse();
    }

    @Test
    public void shouldSetProgressForArtwork() {
        final PlaybackProgress progress = new PlaybackProgress(0, 1000L, Urn.NOT_SET);
        final PlaybackProgressEvent event = PlaybackProgressEvent.create(progress, Urn.NOT_SET);

        presenter.subscribeToEvents();
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(playerArtworkController).setProgress(progress);
    }

    @Test
    public void shouldSetPlaybackStateForArtwork() {
        final PlayStateEvent event = PlayStateEvent.create(PlaybackStateTransition.DEFAULT, 1000L, true, "");

        presenter.subscribeToEvents();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, event);

        verify(playerArtworkController).setPlayState(event);
    }

    @Test
    public void shouldCycleRepeatModeToRepatOneFromRepeatNoneOnClick() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_NONE);

        presenter.repeatClicked(new ImageView(context()));

        verifyRepeatModeChanged(RepeatMode.REPEAT_ONE);
    }

    @Test
    public void shouldCycleRepeatModeToRepeatAllFromRepeatOneOnClick() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_ONE);

        presenter.repeatClicked(new ImageView(context()));

        verifyRepeatModeChanged(RepeatMode.REPEAT_ALL);
    }

    @Test
    public void shouldCycleRepeatModeOnClickOnLastMode() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_ALL);

        presenter.repeatClicked(new ImageView(context()));

        verifyRepeatModeChanged(RepeatMode.REPEAT_NONE);
    }

    @Test
    public void shouldTrackRepeatModeChanges() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_NONE);

        presenter.repeatClicked(new ImageView(context()));

        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_REPEAT);
        assertThat(trackingEvent.playQueueRepeatMode().get()).isEqualTo(RepeatMode.REPEAT_ONE.get());
    }

    @Test
    public void shouldToggleShuffleModeOn() {
        final ToggleButton toggle = new ToggleButton(context());
        toggle.setChecked(true);

        presenter.shuffleClicked(toggle);

        verify(playQueueManager).shuffle();
    }

    @Test
    public void shouldToggleShuffleModeOff() {
        final ToggleButton toggle = new ToggleButton(context());
        toggle.setChecked(false);

        presenter.shuffleClicked(toggle);

        verify(playQueueManager).unshuffle();
    }

    @Test
    public void shouldRemoveATrack() {
        final TrackPlayQueueUIItem trackPlayQueueUIItem = trackPlayQueueUIItemWithPlayState(COMING_UP);
        final PlayQueueItem playQueueItem = trackPlayQueueUIItem.getPlayQueueItem();
        final int position = 0;

        when(adapter.getItems()).thenReturn(Collections.<PlayQueueUIItem>emptyList());
        when(adapter.getItem(position)).thenReturn(trackPlayQueueUIItem);
        when(adapter.getAdapterPosition(playQueueItem)).thenReturn(position);
        when(playQueueOperations.getTracks()).thenReturn(tracksSubject);
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));

        presenter.remove(position);

        verify(adapter).removeItem(position);
        verify(playQueueManager).removeItem(playQueueItem);
        verify(feedbackController).showFeedback(any(Feedback.class));
    }

    @Test
    public void shouldNotRemoveAHeader() {
        final int position = 0;

        when(adapter.getItem(position)).thenReturn(headerItem);

        presenter.remove(position);

        verify(adapter, never()).removeItem(position);
        verify(playQueueManager, never()).removeItem(any(PlayQueueItem.class));
        verify(feedbackController, never()).showFeedback(any(Feedback.class));
    }

    @Test
    public void shouldRebuildPlayQueueUIItems() {
        final TestSubscriber<List<PlayQueueUIItem>> observer = new TestSubscriber<>();
        final Optional<String> contextTitle = Optional.of("Some title");
        final TrackPlayQueueUIItem trackPlayQueueUIItem = trackPlayQueueUIItemWithPlayState(COMING_UP, contextTitle);
        final List<PlayQueueUIItem> items = Collections.<PlayQueueUIItem>singletonList(trackPlayQueueUIItem);

        presenter.rebuildPlayQueueUIItemsObservable(items).subscribe(observer);
        observer.assertValueCount(1);

        final List<PlayQueueUIItem> receivedItems = observer.getOnNextEvents().get(0);
        assertThat(receivedItems).hasSize(3);

        final HeaderPlayQueueUIItem receivedHeader = (HeaderPlayQueueUIItem) receivedItems.get(0);
        assertThat(receivedHeader.getContentTitle()).isEqualTo(contextTitle);

        final TrackPlayQueueUIItem receivedTrack = (TrackPlayQueueUIItem) receivedItems.get(1);
        assertThat(receivedTrack.getContextTitle()).isEqualTo(contextTitle);

        final MagicBoxPlayQueueUIItem receivedMagicBox = (MagicBoxPlayQueueUIItem) receivedItems.get(2);
        assertThat(receivedMagicBox.getKind()).isEqualTo(MAGIC_BOX);
    }

    @Test
    public void shouldEmitUIEventWhenClosing() {
        presenter.closePlayQueue();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_CLOSE.toString());
    }

    @Test
    public void shouldTrackShufflingOn() {
        ToggleButton toggleButton = mock(ToggleButton.class);
        when(toggleButton.isChecked()).thenReturn(true);

        presenter.shuffleClicked(toggleButton);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_SHUFFLE);
        assertThat(event.clickName().get()).isEqualTo(UIEvent.ClickName.SHUFFLE_ON);
    }

    @Test
    public void shouldTrackShufflingOff() {
        ToggleButton toggleButton = mock(ToggleButton.class);
        when(toggleButton.isChecked()).thenReturn(false);

        presenter.shuffleClicked(toggleButton);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_SHUFFLE);
        assertThat(event.clickName().get()).isEqualTo(UIEvent.ClickName.SHUFFLE_OFF);
    }

    @Test
    public void shouldTrackReorder() {
        presenter.moveItems(0, 1);

        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_TRACK_REORDER);
    }

    @Test
    public void shouldTrackRemoval() {
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueUIItemWithPlayState(COMING_UP);
        when(adapter.getItem(2)).thenReturn(upcomingTrack);
        when(playQueueOperations.getTracks()).thenReturn(tracksSubject);
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));

        presenter.remove(2);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UIEvent.Kind.PLAY_QUEUE_TRACK_REMOVE.toString());
    }

    @Test
    public void shouldTrackRemovalUndo() {
        when(adapter.getItem(2)).thenReturn(trackPlayQueueUIItemWithPlayState(COMING_UP));
        when(playQueueOperations.getTracks()).thenReturn(tracksSubject);
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));

        presenter.remove(2);
        feedbackUndo();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UIEvent.Kind.PLAY_QUEUE_TRACK_REMOVE_UNDO.toString());
    }

    private void feedbackUndo() {
        verify(feedbackController).showFeedback(feedbackCaptor.capture());
        final WeakReference<View.OnClickListener> actionListener = feedbackCaptor.getValue().getActionListener();

        if (actionListener != null) {
            actionListener.get().onClick(mock(View.class));
        }
    }

    private void verifyRepeatModeChanged(RepeatMode mode) {
        verify(playQueueManager).setRepeatMode(mode);
        verify(adapter).updateInRepeatMode(mode);
    }

    private TrackPlayQueueUIItem trackPlayQueueUIItemWithPlayState(PlayState playState) {
        return trackPlayQueueUIItemWithPlayState(playState, Optional.<String>absent());
    }

    private HeaderPlayQueueUIItem headerPlayQueueUIItem() {
        return new HeaderPlayQueueUIItem(null,
                                         Optional.<String>absent(),
                                         PlayState.PLAYING,
                                         RepeatMode.REPEAT_ONE);
    }

    private TrackPlayQueueUIItem trackPlayQueueUIItemWithPlayState(PlayState playState, Optional<String> contextTitle) {
        final Urn track = Urn.forTrack(123);
        final TrackPlayQueueUIItem playQueueUIItem = TrackPlayQueueUIItem
                .from(TestPlayQueueItem.createTrack(track),
                      new TrackItem(TestPropertySets.expectedTrackForListItem(track)),
                      context(),
                      contextTitle,
                      RepeatMode.REPEAT_ONE);

        playQueueUIItem.setPlayState(playState);

        return playQueueUIItem;
    }

    private static TrackAndPlayQueueItem trackAndPlayQueueItem(Urn track, PlaybackContext playbackContext) {
        return new TrackAndPlayQueueItem(trackItem(track), createTrackWithContext(track, playbackContext));
    }

    private static TrackItem trackItem(Urn track) {
        return new TrackItem(TestPropertySets.expectedTrackForListItem(track));
    }

    private void setCachedObservables() {
        TrackAndPlayQueueItem trackAndPlayQueueItem1 = trackAndPlayQueueItem(Urn.forTrack(1L), PlaybackContext.create(
                PlaySessionSource.EMPTY));
        TrackAndPlayQueueItem trackAndPlayQueueItem2 = trackAndPlayQueueItem(Urn.forTrack(2L), PlaybackContext.create(
                PlaySessionSource.EMPTY));
        List<TrackAndPlayQueueItem> trackAndPlayQueueItems = Arrays.asList(trackAndPlayQueueItem1, trackAndPlayQueueItem2);


        when(playQueueOperations.getTracks()).thenReturn(Observable.just(trackAndPlayQueueItems));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.<Urn, String>emptyMap()));
    }

}

