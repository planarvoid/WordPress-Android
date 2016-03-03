package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedPromotedPlaylist;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedPromotedTrack;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForWidget;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
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

    private static final Urn WIDGET_TRACK_URN = Urn.forTrack(123L);

    private static PropertySet widgetTrack;

    @Mock private Context context;
    @Mock private PlayerWidgetPresenter playerWidgetPresenter;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackRepository trackRepository;
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
                engagementsTracking);
        when(context.getResources()).thenReturn(resources());
        widgetTrack = expectedTrackForWidget().put(AdProperty.IS_AUDIO_AD, false);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN));
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
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForNewQueueIfCurrentTrackIsNotAudioAd() throws CreateModelException {
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN), Urn.NOT_SET, 0));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForNewQueueIfCurrentTrackIsAudioAd() throws CreateModelException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final PlayQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L), audioAd);

        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack.put(AdProperty.IS_AUDIO_AD, true)));
        controller.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(playQueueItem, Urn.NOT_SET, 0));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForPositionChange() throws CreateModelException {
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN), Urn.NOT_SET, 0));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedPlayableIsCurrentlyPlayingNormalTrack() {
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN));
        when(trackRepository.track(WIDGET_TRACK_URN)).thenReturn(Observable.just(widgetTrack));
        EntityStateChangedEvent event = EntityStateChangedEvent.fromLike(WIDGET_TRACK_URN, true, 1);
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        ArgumentCaptor<PropertySet> captor = ArgumentCaptor.forClass(PropertySet.class);
        verify(playerWidgetPresenter).updateTrackInformation(eq(context), captor.capture());
        assertThat(captor.getValue().get(PlayableProperty.IS_USER_LIKE)).isTrue();
        assertThat(captor.getValue().get(AdProperty.IS_AUDIO_AD)).isFalse();
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedPlayableIsCurrentlyPlayingTrackAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN, AdFixtures.getAudioAd(Urn.forTrack(123L))));
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(true);
        when(trackRepository.track(WIDGET_TRACK_URN)).thenReturn(Observable.just(widgetTrack.put(AdProperty.IS_AUDIO_AD, true)));
        EntityStateChangedEvent event = EntityStateChangedEvent.fromLike(WIDGET_TRACK_URN, true, 1);
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        ArgumentCaptor<PropertySet> captor = ArgumentCaptor.forClass(PropertySet.class);
        verify(playerWidgetPresenter).updateTrackInformation(eq(context), captor.capture());
        assertThat(captor.getValue().get(PlayableProperty.IS_USER_LIKE)).isTrue();
        assertThat(captor.getValue().get(AdProperty.IS_AUDIO_AD)).isTrue();
    }

    @Test
    public void shouldNotUpdatePresenterWhenChangedTrackIsNotCurrentlyPlayingTrack() {
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(false);
        EntityStateChangedEvent event = EntityStateChangedEvent.fromLike(WIDGET_TRACK_URN, true, 1);

        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);
        verify(playerWidgetPresenter, never()).updateTrackInformation(any(Context.class), any(PropertySet.class));
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
    public void shouldUpdatePresenterTrackInformationWhenCurrentTrackIsNotAudioAd() throws CreateModelException {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN));

        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));

        controller.update();

        verify(playerWidgetPresenter).updateTrackInformation(eq(context), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterTrackWithAdInformationWhenCurrentTrackIsAudioAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN, AdFixtures.getAudioAd(Urn.forTrack(123L))));
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack.put(AdProperty.IS_AUDIO_AD, true)));

        controller.update();

        verify(playerWidgetPresenter).updateTrackInformation(eq(context), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsPlayingOnUpdate() {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.<PropertySet>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, true);
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsNotPlayingOnUpdate() {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(false);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.<PropertySet>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, false);
    }

    @Test
    public void toggleLikeActionTriggersToggleLikeOperations() throws CreateModelException {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        controller.handleToggleLikeAction(true);

        verify(likeOperations).toggleLike(Urn.forTrack(123), true);
    }

    @Test
    public void toggleLikeActionShouldEmitLikeUIEventForRegularTrack() {
        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.isTrackFromCurrentPromotedItem(any(Urn.class))).thenReturn(false);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        controller.handleToggleLikeAction(true);

        verify(engagementsTracking).likeTrackUrn(WIDGET_TRACK_URN, true, getWidgetContextMetadata(), null);
    }

    @Test
    public void toggleLikeActionShouldEmitLikeUIEventForPromotedTrack() {
        final PropertySet promotedTrack = expectedPromotedTrack();
        final PromotedTrackItem promotedTrackItem = PromotedTrackItem.from(promotedTrack);
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrackItem);

        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.getCurrentPromotedSourceInfo(promotedTrackItem.getEntityUrn())).thenReturn(promotedSourceInfo);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(promotedTrackItem.getEntityUrn()));
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(promotedTrack));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        controller.handleToggleLikeAction(true);

        verify(engagementsTracking).likeTrackUrn(promotedTrackItem.getEntityUrn(), true, getWidgetContextMetadata(), promotedSourceInfo);
    }


    @Test
    public void toggleLikeActionShouldEmitLikeUIEventForTrackInPromotedPlaylist() {
        final PropertySet promotedPlaylist = expectedPromotedPlaylist();
        final PromotedPlaylistItem promotedPlaylistItem = PromotedPlaylistItem.from(promotedPlaylist);
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedPlaylistItem);

        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.getCurrentPromotedSourceInfo(WIDGET_TRACK_URN)).thenReturn(promotedSourceInfo);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN));
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        controller.handleToggleLikeAction(true);

        verify(engagementsTracking).likeTrackUrn(WIDGET_TRACK_URN, true, getWidgetContextMetadata(), promotedSourceInfo);
    }

    @Test
    public void toggleLikeActionShouldEmitLikeUIEventForTrackNotInButPlayedWithPromotedPlaylist() {
        final PropertySet promotedPlaylist = expectedPromotedPlaylist();
        final PromotedPlaylistItem promotedPlaylistItem = PromotedPlaylistItem.from(promotedPlaylist);
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedPlaylistItem);

        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo("origin_screen", true);
        trackSourceInfo.setPromotedSourceInfo(promotedSourceInfo);

        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.isTrackFromCurrentPromotedItem(any(Urn.class))).thenReturn(false);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(WIDGET_TRACK_URN));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

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
