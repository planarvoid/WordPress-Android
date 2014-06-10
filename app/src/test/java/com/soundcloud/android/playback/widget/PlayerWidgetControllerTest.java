package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.track.LegacyTrackOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlayerWidgetControllerTest {

    private PlayerWidgetController controller;
    private EventMonitor eventMonitor;

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
    @Mock
    private EventBus eventBus;


    @Before
    public void setUp() throws Exception {
        when(context.getApplicationContext()).thenReturn(context);
        controller = new PlayerWidgetController(context,
                playerWidgetPresenter,
                playSessionController,
                playQueueManager,
                trackOperations,
                soundAssocicationOps, eventBus);
        eventMonitor = EventMonitor.on(eventBus);
    }

    @Test
    public void shouldSubscribeToPlayableChangeEvent() throws Exception {
        controller.subscribe();

        eventMonitor.verifySubscribedTo(EventQueue.PLAYABLE_CHANGED);
    }

    @Test
    public void shouldSubscribeToCurrentUserChangedEvent() throws Exception {
        controller.subscribe();

        eventMonitor.verifySubscribedTo(EventQueue.CURRENT_USER_CHANGED);
    }

    @Test
    public void shouldSubscribeToPlaybackStateChangedEvent() throws Exception {
        controller.subscribe();

        eventMonitor.verifySubscribedTo(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void shouldUpdatePresenterOnPlaybackStateChangedEvent() throws Exception {
        controller.subscribe();

        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(PlayaState.PLAYING, Reason.NONE));

        verify(playerWidgetPresenter).updatePlayState(any(Context.class), eq(true));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnPlayQueueEvent() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.just(track));
        controller.subscribe();

        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));

        verify(playerWidgetPresenter).updatePlayableInformation(any(Context.class), eq(track));
    }

    @Test
    public void shouldKeepObservingPlayQueueEventsAfterAnError() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class)))
                .thenReturn(Observable.<Track>error(new Exception()), Observable.just(track));
        controller.subscribe();

        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));
        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));

        verify(playerWidgetPresenter).updatePlayableInformation(any(Context.class), eq(track));
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedTrackIsCurrentlyPlayingTrack() {
        final Track currentTrack = new Track(1L);
        when(playQueueManager.getCurrentTrackId()).thenReturn(currentTrack.getId());
        PlayableChangedEvent event = PlayableChangedEvent.forLike(currentTrack, true);

        controller.subscribe();

        eventMonitor.publish(EventQueue.PLAYABLE_CHANGED, event);
        verify(playerWidgetPresenter).updatePlayableInformation(context, currentTrack);
    }

    @Test
    public void shouldNotPerformPresenterUpdateWhenChangedTrackIsNotCurrentlyPlayingTrack() {
        final Track currentTrack = new Track(1L);
        when(playQueueManager.getCurrentTrackId()).thenReturn(2L);
        PlayableChangedEvent event = PlayableChangedEvent.forLike(currentTrack, true);

        controller.subscribe();

        eventMonitor.publish(EventQueue.PLAYABLE_CHANGED, event);
        verifyZeroInteractions(playerWidgetPresenter);
    }

    @Test
    public void callsResetActionWhenCurrentUserChangedEventReceivedForUserRemoved() {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forLogout();

        controller.subscribe();

        eventMonitor.publish(EventQueue.CURRENT_USER_CHANGED, event);
        verify(playerWidgetPresenter).reset(context);
    }

    @Test
    public void doesNotInteractWithProviderWhenCurrentUserChangedEventReceivedForUserUpdated() {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(new User(1));

        controller.subscribe();

        eventMonitor.publish(EventQueue.CURRENT_USER_CHANGED, event);
        verifyZeroInteractions(playerWidgetPresenter);
    }

    @Test
    public void shouldResetPresenterWhenTheCurrentTrackIsNotSetOnUpdate() throws Exception {
        when(playQueueManager.getCurrentTrackId()).thenReturn((long) Playable.NOT_SET);

        controller.update();

        verify(playerWidgetPresenter).reset(context);
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationWhenCurrentTrackIsSetOnUpdate() throws Exception {
        when(playQueueManager.getCurrentTrackId()).thenReturn(1L);
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        when(trackOperations.loadTrack(eq(1L), any(Scheduler.class))).thenReturn(Observable.from(track));

        controller.update();

        verify(playerWidgetPresenter).updatePlayableInformation(context, track);
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsPlayingOnUpdate() throws Exception {
        when(playSessionController.isPlaying()).thenReturn(true);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.<Track>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, true);
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsNotPlayingOnUpdate() throws Exception {
        when(playSessionController.isPlaying()).thenReturn(false);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.<Track>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, false);
    }

    @Test
    public void shouldSetLikeOnReceivedWidgetLikeChanged() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class)))
                .thenReturn(Observable.from(track));

        controller.handleToggleLikeAction(true);

        verify(soundAssocicationOps).toggleLike(false, track);
    }
}
