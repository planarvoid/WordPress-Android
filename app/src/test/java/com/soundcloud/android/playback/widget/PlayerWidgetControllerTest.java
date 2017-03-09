package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.testsupport.fixtures.PlayableFixtures.expectedPromotedPlaylist;
import static com.soundcloud.android.testsupport.fixtures.PlayableFixtures.expectedPromotedTrack;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.Context;

public class PlayerWidgetControllerTest extends AndroidUnitTest {

    private PlayerWidgetController controller;
    private final TestEventBus eventBus = new TestEventBus();

    private static TrackItem widgetTrack = ModelFixtures.expectedTrackEntityForWidget();
    private static final Urn WIDGET_TRACK_URN = widgetTrack.getUrn();

    @Mock private Context context;
    @Mock private PlayerWidgetPresenter playerWidgetPresenter;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackItemRepository trackRepository;
    @Mock private AdsOperations adsOperations;
    @Mock private LikeOperations likeOperations;
    @Mock private EngagementsTracking engagementsTracking;

    @Before
    public void setUp() {
        when(context.getApplicationContext()).thenReturn(context);
        controller = new PlayerWidgetController(context,
                                                playerWidgetPresenter,
                                                playSessionStateProvider,
                                                playQueueManager,
                                                trackRepository,
                                                eventBus,
                                                likeOperations,
                                                engagementsTracking, ModelFixtures.trackItemCreator());
        when(context.getResources()).thenReturn(resources());
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(widgetTrack.getUrn()));
    }

    @Test
    public void shouldUpdatePlayStateWithNotPlayingOnSubscribingToPlaybackStateChangedQueue() {
        controller.subscribe();

        verify(playerWidgetPresenter).updatePlayState(eq(context), eq(false));
    }

    @Test
    public void shouldUpdatePresenterWithDefaultPlayStateFollowedByReceivedPlayStateOnSubscribeAndReceivePlaybackStateChangedEvent() {
        controller.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());

        InOrder inOrder = Mockito.inOrder(playerWidgetPresenter);
        inOrder.verify(playerWidgetPresenter).updatePlayState(eq(context), eq(false));
        inOrder.verify(playerWidgetPresenter).updatePlayState(eq(context), eq(true));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForNewQueueIfCurrentTrackIsNotAudioAd() {
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN),
                                                                Urn.NOT_SET,
                                                                0));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForNewQueueIfCurrentTrackIsAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final PlayQueueItem playQueueItem = TestPlayQueueItem.createAudioAd(audioAd);

        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(playQueueItem, Urn.NOT_SET, 0));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForPositionChange() throws CreateModelException {
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN),
                                                                       Urn.NOT_SET,
                                                                       0));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedPlayableIsCurrentlyPlayingNormalTrack() {
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN));
        when(playQueueManager.getCurrentItemUrn()).thenReturn(Optional.of(WIDGET_TRACK_URN));
        when(trackRepository.track(WIDGET_TRACK_URN)).thenReturn(Observable.just(widgetTrack));
        LikesStatusEvent event = LikesStatusEvent.create(WIDGET_TRACK_URN, true, 1);
        controller.subscribe();

        eventBus.publish(EventQueue.LIKE_CHANGED, event);

        ArgumentCaptor<TrackItem> captor = ArgumentCaptor.forClass(TrackItem.class);
        verify(playerWidgetPresenter).updateTrackInformation(eq(context), captor.capture());
        assertThat(captor.getValue().isUserLike()).isTrue();
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedPlayableIsCurrentlyPlayingTrackAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(WIDGET_TRACK_URN)));
        when(playQueueManager.getCurrentItemUrn()).thenReturn(Optional.of(WIDGET_TRACK_URN));

        LikesStatusEvent event = LikesStatusEvent.create(WIDGET_TRACK_URN, true, 1);
        controller.subscribe();

        eventBus.publish(EventQueue.LIKE_CHANGED, event);

        verify(playerWidgetPresenter).updateForAudioAd(eq(context));
    }

    @Test
    public void shouldNotUpdatePresenterWhenChangedTrackIsNotCurrentlyPlayingTrack() {
        final Urn currentlyPlayingTrackUrn = Urn.forTrack(111L);
        final Urn likedUrn = Urn.forTrack(222L);
        when(playQueueManager.getCurrentItemUrn()).thenReturn(Optional.of(currentlyPlayingTrackUrn));
        LikesStatusEvent event = LikesStatusEvent.create(likedUrn, true, 1);

        controller.subscribe();

        eventBus.publish(EventQueue.LIKE_CHANGED, event);
        verify(playerWidgetPresenter, never()).updateTrackInformation(any(Context.class), any(TrackItem.class));
    }

    @Test
    public void callsResetActionWhenCurrentUserChangedEventReceivedForUserRemoved() {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forLogout();

        controller.subscribe();

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, event);
        verify(playerWidgetPresenter).reset(context);
    }

    @Test
    public void doesNotResetPresentationWhenCurrentUserChangedEventReceivedForUserUpdated() {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(Urn.forUser(123));

        controller.subscribe();

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, event);
        verify(playerWidgetPresenter, never()).reset(any(Context.class));
    }

    @Test
    public void shouldUpdatePresenterTrackInformationWhenCurrentTrackIsNotAudioAd() {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN));

        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));

        controller.update();

        verify(playerWidgetPresenter).updateTrackInformation(eq(context), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterWithAdInformationWhenCurrentTrackIsAudioAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L))));
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));

        controller.update();

        verify(playerWidgetPresenter).updateForAudioAd(eq(context));
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsPlayingOnUpdate() {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, true);
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsNotPlayingOnUpdate() {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(false);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, false);
    }

    @Test
    public void toggleLikeActionTriggersToggleLikeOperations() throws CreateModelException {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.empty());

        controller.handleToggleLikeAction(true);

        verify(likeOperations).toggleLike(WIDGET_TRACK_URN, true);
    }

    @Test
    public void toggleLikeActionShouldEmitLikeUIEventForRegularTrack() {
        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.isTrackFromCurrentPromotedItem(any(Urn.class))).thenReturn(false);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.empty());

        controller.handleToggleLikeAction(true);

        verify(engagementsTracking).likeTrackUrn(WIDGET_TRACK_URN, true, getWidgetContextMetadata(), null);
    }

    @Test
    public void toggleLikeActionShouldEmitLikeUIEventForPromotedTrack() {
        final TrackItem promotedTrackItem = expectedPromotedTrack();
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrackItem);

        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.getCurrentPromotedSourceInfo(promotedTrackItem.getUrn())).thenReturn(promotedSourceInfo);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(promotedTrackItem.getUrn()));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.empty());

        controller.handleToggleLikeAction(true);

        verify(engagementsTracking).likeTrackUrn(promotedTrackItem.getUrn(),
                                                 true,
                                                 getWidgetContextMetadata(),
                                                 promotedSourceInfo);
    }


    @Test
    public void toggleLikeActionShouldEmitLikeUIEventForTrackInPromotedPlaylist() {
        final PlaylistItem promotedPlaylistItem = expectedPromotedPlaylist();
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedPlaylistItem);

        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.getCurrentPromotedSourceInfo(WIDGET_TRACK_URN)).thenReturn(promotedSourceInfo);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN));
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.empty());

        controller.handleToggleLikeAction(true);

        verify(engagementsTracking).likeTrackUrn(WIDGET_TRACK_URN,
                                                 true,
                                                 getWidgetContextMetadata(),
                                                 promotedSourceInfo);
    }

    @Test
    public void toggleLikeActionShouldEmitLikeUIEventForTrackNotInButPlayedWithPromotedPlaylist() {
        final PlaylistItem promotedPlaylistItem = expectedPromotedPlaylist();
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedPlaylistItem);

        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo("origin_screen", true);
        trackSourceInfo.setPromotedSourceInfo(promotedSourceInfo);

        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.isTrackFromCurrentPromotedItem(any(Urn.class))).thenReturn(false);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.empty());

        controller.handleToggleLikeAction(true);

        verify(engagementsTracking).likeTrackUrn(WIDGET_TRACK_URN, true, getWidgetContextMetadata(), null);
    }

    private EventContextMetadata getWidgetContextMetadata() {
        return EventContextMetadata.builder()
                                   .invokerScreen("widget")
                                   .contextScreen("context_screen")
                                   .pageName("widget")
                                   .build();
    }

}
