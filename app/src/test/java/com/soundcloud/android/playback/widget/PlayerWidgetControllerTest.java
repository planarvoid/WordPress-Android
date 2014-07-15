package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.tracks.LegacyTrackOperations;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Scheduler;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlayerWidgetControllerTest {

    private PlayerWidgetController controller;
    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private Context context;
    @Mock
    private PlayerWidgetPresenter playerWidgetPresenter;
    @Mock
    private PlaySessionController playSessionController;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private LegacyTrackOperations trackOperations;
    @Mock
    private SoundAssociationOperations soundAssocicationOps;


    @Before
    public void setUp() {
        when(context.getApplicationContext()).thenReturn(context);
        controller = new PlayerWidgetController(context,
                playerWidgetPresenter,
                playSessionController,
                playQueueManager,
                trackOperations,
                soundAssocicationOps, eventBus);
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
    public void shouldUpdatePresenterPlayableInformationOnNewQueuePlayQueueEvent() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.just(track));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));

        verify(playerWidgetPresenter).updatePlayableInformation(any(Context.class), eq(track));
    }

    @Test
    public void shouldNotUpdatePresenterPlayableInformationOnPlayQueueUpdateEvent() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.just(track));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(track.getUrn()));

        verify(playerWidgetPresenter, never()).updatePlayableInformation(any(Context.class), eq(track));
    }


    @Test
    public void shouldKeepObservingPlayQueueEventsAfterAnError() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class)))
                .thenReturn(Observable.<PublicApiTrack>error(new Exception()), Observable.just(track));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));

        verify(playerWidgetPresenter).updatePlayableInformation(any(Context.class), eq(track));
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedTrackIsCurrentlyPlayingTrack() {
        final PublicApiTrack currentTrack = new PublicApiTrack(1L);
        when(playQueueManager.getCurrentTrackId()).thenReturn(currentTrack.getId());
        PlayableChangedEvent event = PlayableChangedEvent.forLike(currentTrack, true);

        controller.subscribe();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);
        verify(playerWidgetPresenter).updatePlayableInformation(context, currentTrack);
    }

    @Test
    public void shouldNotUpdatePresenterWhenChangedTrackIsNotCurrentlyPlayingTrack() {
        final PublicApiTrack currentTrack = new PublicApiTrack(1L);
        when(playQueueManager.getCurrentTrackId()).thenReturn(2L);
        PlayableChangedEvent event = PlayableChangedEvent.forLike(currentTrack, true);

        controller.subscribe();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);
        verify(playerWidgetPresenter, never()).updatePlayableInformation(any(Context.class), any(Playable.class));
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
        when(playQueueManager.getCurrentTrackId()).thenReturn((long) Playable.NOT_SET);

        controller.update();

        verify(playerWidgetPresenter).reset(context);
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationWhenCurrentTrackIsSetOnUpdate() throws CreateModelException {
        when(playQueueManager.getCurrentTrackId()).thenReturn(1L);
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        when(trackOperations.loadTrack(eq(1L), any(Scheduler.class))).thenReturn(Observable.from(track));

        controller.update();

        verify(playerWidgetPresenter).updatePlayableInformation(context, track);
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsPlayingOnUpdate() {
        when(playSessionController.isPlaying()).thenReturn(true);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.<PublicApiTrack>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, true);
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsNotPlayingOnUpdate() {
        when(playSessionController.isPlaying()).thenReturn(false);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.<PublicApiTrack>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, false);
    }

    @Test
    public void shouldSetLikeOnReceivedWidgetLikeChanged() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class)))
                .thenReturn(Observable.from(track));
        when(soundAssocicationOps.toggleLike(false, track)).thenReturn(Observable.<SoundAssociation>never());

        controller.handleToggleLikeAction(true);

        verify(soundAssocicationOps).toggleLike(false, track);
    }
}
