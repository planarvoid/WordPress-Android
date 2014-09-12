package com.soundcloud.android.playback.widget;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.audioAdProperties;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForWidget;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
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

    private static final Urn WIDGET_TRACK_URN = Urn.forTrack(123L);

    private static PropertySet widgetTrack;
    private static PropertySet widgetTrackWithAd;

    @Mock private Context context;
    @Mock private PlayerWidgetPresenter playerWidgetPresenter;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackOperations trackOperations;
    @Mock private SoundAssociationOperations soundAssociationOps;
    @Mock private AdsOperations adsOperations;

    @Before
    public void setUp() {
        when(context.getApplicationContext()).thenReturn(context);
        controller = new PlayerWidgetController(context,
                playerWidgetPresenter,
                playSessionStateProvider,
                playQueueManager,
                trackOperations,
                soundAssociationOps, eventBus);
        when(context.getResources()).thenReturn(Robolectric.application.getResources());
        widgetTrack = expectedTrackForWidget();
        widgetTrackWithAd = expectedTrackForWidget().merge(audioAdProperties(Urn.forTrack(123L)));
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
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForNewQueueIfCurrentTrackIsNotAudioAd() throws CreateModelException {
        when(trackOperations.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(WIDGET_TRACK_URN));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForNewQueueIfCurrentTrackIsAudioAd() throws CreateModelException {
        final PropertySet audioAdProperties = audioAdProperties(Urn.forTrack(123L));

        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAdProperties);
        when(trackOperations.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(WIDGET_TRACK_URN, audioAdProperties));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack.merge(audioAdProperties)));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForPositionChange() throws CreateModelException {
        when(trackOperations.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(WIDGET_TRACK_URN));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedPlayableIsCurrentlyPlayingNormalTrack() {
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(WIDGET_TRACK_URN);
        when(playQueueManager.getCurrentMetaData()).thenReturn(PropertySet.create());
        when(trackOperations.track(WIDGET_TRACK_URN)).thenReturn(Observable.just(widgetTrack));
        PlayableUpdatedEvent event = PlayableUpdatedEvent.forLike(WIDGET_TRACK_URN, true, 1);
        controller.subscribe();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);

        ArgumentCaptor<PropertySet> captor = ArgumentCaptor.forClass(PropertySet.class);
        verify(playerWidgetPresenter).updateTrackInformation(eq(context), captor.capture());
        expect(captor.getValue().get(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(captor.getValue().contains(AdProperty.AD_URN)).toBeFalse();
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedPlayableIsCurrentlyPlayingTrackAd() {
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAdProperties(Urn.forTrack(123L)));
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(WIDGET_TRACK_URN);
        when(trackOperations.track(WIDGET_TRACK_URN)).thenReturn(Observable.just(widgetTrack));
        PlayableUpdatedEvent event = PlayableUpdatedEvent.forLike(WIDGET_TRACK_URN, true, 1);
        controller.subscribe();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);

        ArgumentCaptor<PropertySet> captor = ArgumentCaptor.forClass(PropertySet.class);
        verify(playerWidgetPresenter).updateTrackInformation(eq(context), captor.capture());
        expect(captor.getValue().get(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(captor.getValue().contains(AdProperty.AD_URN)).toBeTrue();
    }

    @Test
    public void shouldNotUpdatePresenterWhenChangedTrackIsNotCurrentlyPlayingTrack() {
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(false);
        PlayableUpdatedEvent event = PlayableUpdatedEvent.forLike(WIDGET_TRACK_URN, true, 1);

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
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.NOT_SET);

        controller.update();

        verify(playerWidgetPresenter).reset(context);
    }

    @Test
    public void shouldUpdatePresenterTrackInformationWhenCurrentTrackIsNotAudioAd() throws CreateModelException {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playQueueManager.getCurrentMetaData()).thenReturn(PropertySet.create());
        when(trackOperations.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));

        controller.update();

        verify(playerWidgetPresenter).updateTrackInformation(eq(context), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterTrackWithAdInformationWhenCurrentTrackIsAudioAd() {
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAdProperties(Urn.forTrack(123L)));
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(WIDGET_TRACK_URN);
        when(trackOperations.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));

        controller.update();

        verify(playerWidgetPresenter).updateTrackInformation(eq(context), eq(widgetTrackWithAd));
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsPlayingOnUpdate() {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(trackOperations.track(any(Urn.class))).thenReturn(Observable.<PropertySet>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, true);
    }

    @Test
    public void shouldUpdatePresenterWithCurrentPlayStateIfIsNotPlayingOnUpdate() {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(playSessionStateProvider.isPlaying()).thenReturn(false);
        when(trackOperations.track(any(Urn.class))).thenReturn(Observable.<PropertySet>empty());

        controller.update();

        verify(playerWidgetPresenter).updatePlayState(context, false);
    }

    @Test
    public void shouldSetLikeOnReceivedWidgetLikeChanged() throws CreateModelException {
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(trackOperations.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        when(soundAssociationOps.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>never());

        controller.handleToggleLikeAction(true);

        verify(soundAssociationOps).toggleLike(WIDGET_TRACK_URN, true);
    }

}
