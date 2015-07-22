package com.soundcloud.android.playback.widget;

import static com.pivotallabs.greatexpectations.Expect.expect;
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
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
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
    @Mock private TrackRepository trackRepository;
    @Mock private AdsOperations adsOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private LikeOperations likeOperations;

    @Before
    public void setUp() {
        when(context.getApplicationContext()).thenReturn(context);
        controller = new PlayerWidgetController(context,
                playerWidgetPresenter,
                playSessionStateProvider,
                playQueueManager,
                trackRepository,
                eventBus,
                likeOperations);
        when(context.getResources()).thenReturn(Robolectric.application.getResources());
        widgetTrack = expectedTrackForWidget();
        widgetTrackWithAd = expectedTrackForWidget().merge(audioAdProperties(Urn.forTrack(123L)));
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(WIDGET_TRACK_URN);
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

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(WIDGET_TRACK_URN));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForNewQueueIfCurrentTrackIsAudioAd() throws CreateModelException {
        final PropertySet audioAdProperties = audioAdProperties(Urn.forTrack(123L));

        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAdProperties);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(WIDGET_TRACK_URN, audioAdProperties));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack.merge(audioAdProperties)));
    }

    @Test
    public void shouldUpdatePresenterPlayableInformationOnCurrentPlayQueueTrackEventForPositionChange() throws CreateModelException {
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        controller.subscribe();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(WIDGET_TRACK_URN));

        verify(playerWidgetPresenter).updateTrackInformation(any(Context.class), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedPlayableIsCurrentlyPlayingNormalTrack() {
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(WIDGET_TRACK_URN);
        when(playQueueManager.getCurrentMetaData()).thenReturn(PropertySet.create());
        when(trackRepository.track(WIDGET_TRACK_URN)).thenReturn(Observable.just(widgetTrack));
        EntityStateChangedEvent event = EntityStateChangedEvent.fromLike(WIDGET_TRACK_URN, true, 1);
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        ArgumentCaptor<PropertySet> captor = ArgumentCaptor.forClass(PropertySet.class);
        verify(playerWidgetPresenter).updateTrackInformation(eq(context), captor.capture());
        expect(captor.getValue().get(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(captor.getValue().contains(AdProperty.AUDIO_AD_URN)).toBeFalse();
    }

    @Test
    public void shouldUpdatePresenterPlayStateInformationWhenChangedPlayableIsCurrentlyPlayingTrackAd() {
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAdProperties(Urn.forTrack(123L)));
        when(playQueueManager.isCurrentTrack(WIDGET_TRACK_URN)).thenReturn(true);
        when(trackRepository.track(WIDGET_TRACK_URN)).thenReturn(Observable.just(widgetTrack));
        EntityStateChangedEvent event = EntityStateChangedEvent.fromLike(WIDGET_TRACK_URN, true, 1);
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        ArgumentCaptor<PropertySet> captor = ArgumentCaptor.forClass(PropertySet.class);
        verify(playerWidgetPresenter).updateTrackInformation(eq(context), captor.capture());
        expect(captor.getValue().get(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(captor.getValue().contains(AdProperty.AUDIO_AD_URN)).toBeTrue();
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
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(ModelFixtures.create(PublicApiUser.class));

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
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));

        controller.update();

        verify(playerWidgetPresenter).updateTrackInformation(eq(context), eq(widgetTrack));
    }

    @Test
    public void shouldUpdatePresenterTrackWithAdInformationWhenCurrentTrackIsAudioAd() {
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAdProperties(Urn.forTrack(123L)));
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));

        controller.update();

        verify(playerWidgetPresenter).updateTrackInformation(eq(context), eq(widgetTrackWithAd));
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
    public void toggleLikeActionShouldEmitLikeUIEvent() {
        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(true);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(widgetTrack));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        controller.handleToggleLikeAction(true);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true, "widget", "context_screen", WIDGET_TRACK_URN);
        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toEqual(expectedEvent.getKind());
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
    }
}
