package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem.createTrackWithContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.playback.PlaybackStateProvider;
import com.soundcloud.android.playback.PlaylistExploder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayQueuePresenterTest extends AndroidUnitTest {

    private final PublishSubject<List<TrackAndPlayQueueItem>> tracksSubject = PublishSubject.create();
    @Mock private PlayQueueView playQueueViewContract;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlaySessionController playSessionController;
    @Mock private PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory;
    @Mock private PlaybackStateProvider playbackStateProvider;
    @Mock private PlayQueueUIItem item;
    @Mock private PlaylistExploder playlistExploder;

    private PlayQueuePresenter presenter;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        final PlayQueueUIItemMapper playQueueUIItemMapper = new PlayQueueUIItemMapper(context(), playQueueManager);

        presenter = new PlayQueuePresenter(
                playQueueManager,
                playbackStateProvider,
                playSessionController,
                playQueueOperations,
                playlistExploder,
                eventBus,
                playQueueUIItemMapper);
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_NONE);
        when(playQueueManager.isShuffled()).thenReturn(false);
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(item.isTrack()).thenReturn(true);
        when(playbackStateProvider.isSupposedToBePlaying()).thenReturn(true);
        setCachedObservables();
        presenter.attachView(playQueueViewContract);
    }

    @Test
    public void shouldNotCallToContractWhenDetached() {
        presenter.detachContract();

        reset(playQueueViewContract);

        presenter.repeatClicked();
        presenter.shuffleClicked(true);

        verifyZeroInteractions(playQueueViewContract);
    }

    @Test
    public void shouldSetViewOnAttach() {
        verify(playQueueViewContract).showLoadingIndicator();
        verify(playQueueViewContract).setItems(any());
        verify(playQueueViewContract).setShuffledState(false);
        verify(playQueueViewContract).setRepeatNone();
        verify(playQueueViewContract).scrollTo(0);
        verify(playQueueViewContract).removeLoadingIndicator();
    }

    @Test
    public void shouldSetDefaultPlayerStrip() {
        presenter.trackClicked(1);
        verify(playQueueViewContract).setDefaultPlayerStrip();
    }

    @Test
    public void shouldSetGoPlayerStrip() {
        presenter.trackClicked(3);
        verify(playQueueViewContract).setGoPlayerStrip();
    }

    @Test
    public void shouldScrollToPositionOnNext() {
        presenter.onNextClick();

        verify(playQueueViewContract, times(2)).scrollTo(0);
    }

    @Test
    public void shouldSwitchItems() {
        presenter.switchItems(1, 3);

        verify(playQueueViewContract).switchItems(1, 3);
    }

    @Test
    public void returnTrueWhenUpcomingTrack() {
        assertThat(presenter.isRemovable(2)).isTrue();
    }

    @Test
    public void returnFalseWhenCurrentTrack() {
        presenter.trackClicked(2);

        assertThat(presenter.isRemovable(2)).isFalse();
    }

    @Test
    public void returnFalseWhenPlayedTrack() {
        presenter.trackClicked(2);

        assertThat(presenter.isRemovable(1)).isFalse();
    }

    @Test
    public void shouldSetPlayStateWhenTrack() {
        presenter.trackClicked(2);

        verify(playQueueManager).setCurrentPlayQueueItem(any(PlayQueueItem.class));
    }

    @Test
    public void shouldTogglePlayBack() {
        when(playSessionController.isPlayingCurrentPlayQueueItem()).thenReturn(true);

        presenter.trackClicked(2);

        verify(playSessionController).togglePlayback();
    }

    @Test
    public void shouldStartPlayback() {
        when(playSessionController.isPlayingCurrentPlayQueueItem()).thenReturn(false);

        presenter.trackClicked(2);

        verify(playSessionController).play();
    }

    @Test
    public void shouldUpdateAfterItemAdded() {
        setCachedObservables();
        final PlayQueueEvent event = PlayQueueEvent.fromQueueInsert(Urn.NOT_SET);

        eventBus.publish(EventQueue.PLAY_QUEUE, event);

        verify(playQueueViewContract, times(2)).setItems(anyList());
    }

    @Test
    public void shouldSubscribeToPlayQueueChangedAndFilterOutItemChanges() {
        reset(playQueueViewContract);

        when(playQueueOperations.getTracks()).thenReturn(tracksSubject);
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));
        final PlayQueueEvent event = PlayQueueEvent.fromQueueUpdateMoved(Urn.NOT_SET);

        eventBus.publish(EventQueue.PLAY_QUEUE, event);

        verify(playQueueViewContract, never()).setItems(anyList());
    }

    @Test
    public void shouldSetRepeatModeNone() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_ALL);

        presenter.repeatClicked();

        verify(playQueueManager).setRepeatMode(RepeatMode.REPEAT_NONE);
        verify(playQueueViewContract, times(2)).setRepeatNone();
    }

    @Test
    public void shouldSetRepeatModeOne() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_NONE);

        presenter.repeatClicked();

        verify(playQueueManager).setRepeatMode(RepeatMode.REPEAT_ONE);
        verify(playQueueViewContract, times(1)).setRepeatOne();
    }

    @Test
    public void shouldSetRepeatModeAll() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_ONE);

        presenter.repeatClicked();

        verify(playQueueManager).setRepeatMode(RepeatMode.REPEAT_ALL);
        verify(playQueueViewContract, times(1)).setRepeatAll();
    }

    @Test
    public void shouldTrackRepeatModeChanges() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_NONE);

        presenter.repeatClicked();

        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_REPEAT);
        assertThat(trackingEvent.playQueueRepeatMode().get()).isEqualTo(RepeatMode.REPEAT_ONE.get());
    }

    @Test
    public void shouldToggleShuffleModeOn() {
        presenter.shuffleClicked(true);

        verify(playQueueManager).shuffle();
    }

    @Test
    public void shouldToggleShuffleModeOff() {
        presenter.shuffleClicked(false);

        verify(playQueueManager).unshuffle();
    }

    @Test
    public void shouldRemoveATrack() {
        reset(playQueueViewContract);

        final int position = 1;

        presenter.remove(position);

        verify(playQueueManager).removeItem(any());
        verify(playQueueViewContract).showUndo();
        verify(playQueueViewContract).setItems(anyList());
    }

    @Test
    public void shouldNotRemoveAHeader() {
        reset(playQueueViewContract);

        presenter.remove(0);

        verify(playQueueManager, never()).removeItem(any(PlayQueueItem.class));
        verify(playQueueViewContract, never()).showUndo();
        verify(playQueueViewContract, never()).setItems(anyList());
    }

    @Test
    public void shouldEmitUIEventWhenClosing() {
        presenter.closePlayQueue();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_CLOSE.toString());
    }

    @Test
    public void shouldTrackShufflingOn() {
        presenter.shuffleClicked(true);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_SHUFFLE);
        assertThat(event.clickName().get()).isEqualTo(UIEvent.ClickName.SHUFFLE_ON);
    }

    @Test
    public void shouldTrackShufflingOff() {
        presenter.shuffleClicked(false);

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
    public void shouldReorderInPlayQueueManager() {
        presenter.moveItems(0, 2);

        verify(playQueueManager).moveItem(0, 1);
    }

    @Test
    public void shouldTrackRemoval() {
        when(playQueueOperations.getTracks()).thenReturn(tracksSubject);
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));

        presenter.remove(2);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UIEvent.Kind.PLAY_QUEUE_TRACK_REMOVE.toString());
    }

    @Test
    public void shouldTrackRemovalUndo() {
        when(playQueueOperations.getTracks()).thenReturn(tracksSubject);
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));

        presenter.remove(2);
        presenter.undoClicked();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UIEvent.Kind.PLAY_QUEUE_TRACK_REMOVE_UNDO.toString());
    }

    @Test
    public void shouldExplodePlaylistsWhenScrollingUp() {
        presenter.scrollUp(5);

        verify(playlistExploder).explodePlaylists(0, 5);
    }

    @Test
    public void shouldResolvePostion() {
        presenter.scrollUp(0);

        verify(playlistExploder).explodePlaylists(0, 5);
    }

    @Test
    public void shouldExplodePlaylistsWhenScrollingDown() {
        presenter.scrollDown(5);

        verify(playlistExploder).explodePlaylists(5, 5);
    }

    public void shouldAddTrackOnUndo() {
        presenter.remove(2);
        reset(playQueueViewContract);
        presenter.undoClicked();

        verify(playQueueManager).insertItemAtPosition(eq(0), any());
        verify(playQueueViewContract).setItems(anyList());
    }

    @Test
    public void shouldNotShowLoadingIndicatorOnRotation() {
        presenter.detachContract();

        reset(playQueueViewContract);

        presenter.attachView(playQueueViewContract);

        verify(playQueueViewContract, never()).showLoadingIndicator();
    }

    @Test
    public void shouldMoveToNextRecommendationWhenMagicBoxClicked() {
        presenter.magicBoxClicked();

        verify(playQueueManager).moveToNextRecommendationItem();
    }

    @Test
    public void shouldSetAutoPlayWhenMagicBoxToggled() {
        presenter.magicBoxToggled(false);
        presenter.magicBoxToggled(true);

        verify(playQueueManager).setAutoPlay(false);
        verify(playQueueManager).setAutoPlay(true);
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
                      TestPropertySets.expectedTrackForListItem(track),
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
        return TestPropertySets.expectedTrackForListItem(track);
    }

    private void setCachedObservables() {
        TrackAndPlayQueueItem trackAndPlayQueueItem1 = trackAndPlayQueueItem(Urn.forTrack(1L), PlaybackContext.create(
                PlaySessionSource.EMPTY));
        TrackAndPlayQueueItem trackAndPlayQueueItem2 = trackAndPlayQueueItem(Urn.forTrack(2L), PlaybackContext.create(
                PlaySessionSource.EMPTY));
        TrackItem higherTierTrack = TestPropertySets.highTierTrack();
        TrackAndPlayQueueItem trackAndPlayQueueItem3 = new TrackAndPlayQueueItem(higherTierTrack, createTrackWithContext(Urn.forTrack(3L), PlaybackContext.create(PlaySessionSource.EMPTY)));
        List<TrackAndPlayQueueItem> trackAndPlayQueueItems = Arrays.asList(trackAndPlayQueueItem1, trackAndPlayQueueItem2, trackAndPlayQueueItem3);


        when(playQueueOperations.getTracks()).thenReturn(Observable.just(trackAndPlayQueueItems));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));
    }

}

