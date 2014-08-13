package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlayerWidgetControllerTest {

    private PlayerWidgetController controller;
    private final TestEventBus eventBus = new TestEventBus();
    private final PropertySet WIDGET_TRACK = TestPropertySets.forWidgetTrack();
    private final TrackUrn WIDGET_TRACK_URN = Urn.forTrack(123L);

    @Mock private Context context;
    @Mock private PlayerWidgetPresenter playerWidgetPresenter;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackOperations trackOperations;
    @Mock private SoundAssociationOperations soundAssociationOps;

    @Before
    public void setUp() {
        when(context.getApplicationContext()).thenReturn(context);
        controller = new PlayerWidgetController(context,
                playerWidgetPresenter,
                playSessionStateProvider,
                playQueueManager,
                trackOperations,
                soundAssociationOps, eventBus);
    }

    @Test
    public void shouldUpdatePlayStateWithNotPlayingOnSubscribingToPlaybackStateChangedQueue() {
        controller.subscribe();

        verify(playerWidgetPresenter).updatePlayState(eq(context), eq(false));
    }

    @Test
    public void shouldUpdatePresenterWithDefaultPlayStateFollowedByReceivedPlayStateOnSubscribeAndReceivePlaybackStateChangedEvent() {
        controller.subscribe();

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.PLAYING, Reason.NONE));

        InOrder inOrder = Mockito.inOrder(playerWidgetPresenter);
        inOrder.verify(playerWidgetPresenter).updatePlayState(eq(context), eq(false));
        inOrder.verify(playerWidgetPresenter).updatePlayState(eq(context), eq(true));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForNewQueue() throws CreateModelException {
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.just(WIDGET_TRACK));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(WIDGET_TRACK_URN));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(WIDGET_TRACK));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForPositionChange() throws CreateModelException {
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.just(WIDGET_TRACK));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(WIDGET_TRACK_URN));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(WIDGET_TRACK));
    }

    @Test
    public void shouldKeepObservingPlayQueueEventsAfterAnError() throws CreateModelException {
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.<PropertySet>error(new Exception()), Observable.just(WIDGET_TRACK));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(WIDGET_TRACK_URN));
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(WIDGET_TRACK_URN));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(WIDGET_TRACK));
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedTrackIsCurrentlyPlayingTrack() {
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(WIDGET_TRACK_URN);
        when(trackOperations.track(WIDGET_TRACK_URN)).thenReturn(Observable.just(WIDGET_TRACK));
        PlayableChangedEvent event = PlayableChangedEvent.forLike(WIDGET_TRACK_URN, true, 1);
        controller.subscribe();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);

        ArgumentCaptor<PropertySet> captor = ArgumentCaptor.forClass(PropertySet.class);
        verify(playerWidgetPresenter).updateTrackInformation(eq(context), captor.capture());
        expect(captor.getValue().get(PlayableProperty.IS_LIKED)).toBeTrue();
    }

    @Test
    public void shouldNotUpdatePresenterWhenChangedTrackIsNotCurrentlyPlayingTrack() {
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(false);
        PlayableChangedEvent event = PlayableChangedEvent.forLike(WIDGET_TRACK_URN, true, 1);

        controller.subscribe();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);
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
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(new PublicApiUser(1));

        controller.subscribe();

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, event);
        verify(playerWidgetPresenter, never()).reset(any(Context.class));
    }

    @Test
    public void shouldResetPresenterWhenTheCurrentTrackIsNotSetOnUpdate() {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TrackUrn.NOT_SET);

        controller.update();

        verify(playerWidgetPresenter).reset(context);
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationWhenCurrentTrackIsSetOnUpdate() throws CreateModelException {
        when(playQueueManager.isCurrentTrack(any(TrackUrn.class))).thenReturn(true);
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.just(WIDGET_TRACK));

        controller.update();

        verify(playerWidgetPresenter).updateTrackInformation(context, WIDGET_TRACK);
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsPlayingOnUpdate() {
        when(playQueueManager.isCurrentTrack(any(TrackUrn.class))).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.<PropertySet>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, true);
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsNotPlayingOnUpdate() {
        when(playQueueManager.isCurrentTrack(any(TrackUrn.class))).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(false);
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.<PropertySet>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, false);
    }

    @Test
    public void shouldSetLikeOnReceivedWidgetLikeChanged() throws CreateModelException {
        when(playQueueManager.isCurrentTrack(any(TrackUrn.class))).thenReturn(true);
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.just(WIDGET_TRACK));
        when(soundAssociationOps.toggleLike(any(TrackUrn.class), anyBoolean())).thenReturn(Observable.<PropertySet>never());

        controller.handleToggleLikeAction(true);

        verify(soundAssociationOps).toggleLike(WIDGET_TRACK_URN, false);
    }

}
