package com.soundcloud.android.playback.views;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.RxTestHelper.mockObservable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventBus2;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.EventExpectation;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ToggleButton;

@RunWith(SoundCloudTestRunner.class)
public class PlayableControllerTest {

    private PlayableController controller;
    private ViewGroup rootView;

    @Mock
    private SoundAssociationOperations soundAssocOps;
    @Mock
    private Context context;
    @Mock
    private EventBus2 eventBus;

    private Subscription eventSubscription = Subscriptions.empty();

    @Before
    public void setup() {
        LayoutInflater inflater = (LayoutInflater) Robolectric.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = (ViewGroup) inflater.inflate(R.layout.player_action_bar, null);
        controller = new PlayableController(context, eventBus, soundAssocOps, null);
        controller.setLikeButton((ToggleButton) rootView.findViewById(R.id.toggle_like));
        controller.setRepostButton((ToggleButton) rootView.findViewById(R.id.toggle_repost));
        controller.setShareButton((ImageButton) rootView.findViewById(R.id.btn_share));
    }

    @After
    public void tearDown() throws Exception {
        controller.stopListeningForChanges();
        eventSubscription.unsubscribe();
    }

    @Test
    public void shouldShortenCountsOnToggleButtons() {
        expect(controller.labelForCount(999)).toEqual("999");
        expect(controller.labelForCount(1000)).toEqual("1k+");
        expect(controller.labelForCount(1999)).toEqual("1k+");
        expect(controller.labelForCount(2000)).toEqual("2k+");
        expect(controller.labelForCount(9999)).toEqual("9k+");
        expect(controller.labelForCount(10000)).toEqual("9k+"); // 4 chars would make the text spill over again
    }

    @Test
    public void shouldPublishUIEventWhenLikingPlayable() {
        controller.setPlayable(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        eventSubscription = EventBus.UI.subscribe(eventObserver);

        when(soundAssocOps.toggleLike(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(new SoundAssociation(new Track())));
        rootView.findViewById(R.id.toggle_like).performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.LIKE);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlayable() {
        controller.setPlayable(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        eventSubscription = EventBus.UI.subscribe(eventObserver);

        when(soundAssocOps.toggleLike(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(new SoundAssociation(new Track())));
        ToggleButton likeToggle = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        likeToggle.setChecked(true);
        likeToggle.performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.UNLIKE);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        controller.setPlayable(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        eventSubscription = EventBus.UI.subscribe(eventObserver);

        when(soundAssocOps.toggleRepost(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(new SoundAssociation(new Track())));
        rootView.findViewById(R.id.toggle_repost).performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.REPOST);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        controller.setPlayable(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        eventSubscription = EventBus.UI.subscribe(eventObserver);

        when(soundAssocOps.toggleRepost(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(new SoundAssociation(new Track())));
        ToggleButton repostToggle = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        repostToggle.setChecked(true);
        repostToggle.performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.UNREPOST);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }


    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        controller.setPlayable(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        eventSubscription = EventBus.UI.subscribe(eventObserver);

        rootView.findViewById(R.id.btn_share).performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.SHARE);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        Observer<UIEvent> eventObserver = mock(Observer.class);
        eventSubscription = EventBus.UI.subscribe(eventObserver);

        rootView.findViewById(R.id.btn_share).performClick();

        verify(eventObserver, never()).onNext(any(UIEvent.class));
    }

    @Test
    public void shouldLikeTrackWhenCheckingLikeButton() {
        Track track = new Track();
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();

        track.user_like = true;
        when(soundAssocOps.toggleLike(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(new SoundAssociation(track)));

        likeButton.performClick();

        verify(soundAssocOps).toggleLike(eq(true), refEq(track));
        expect(likeButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldResetLikeButtonToPreviousStateWhenLikingFails() {
        Track track = new Track();
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();

        when(soundAssocOps.toggleLike(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.<SoundAssociation>error(new Exception()));

        likeButton.performClick();

        verify(soundAssocOps).toggleLike(eq(true), refEq(track));
        expect(likeButton.isChecked()).toBeFalse();
        expect(likeButton.isEnabled()).toBeTrue();
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        Track track = new Track();
        controller.setPlayable(track);

        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        track.user_repost = true;
        when(soundAssocOps.toggleRepost(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(new SoundAssociation(track)));

        repostButton.performClick();

        verify(soundAssocOps).toggleRepost(eq(true), refEq(track));
        expect(repostButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldResetRepostButtonToPreviousStateWhenRepostingFails() {
        Track track = new Track();
        controller.setPlayable(track);

        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        when(soundAssocOps.toggleRepost(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.<SoundAssociation>error(new Exception()));

        repostButton.performClick();

        verify(soundAssocOps).toggleRepost(eq(true), refEq(track));
        expect(repostButton.isChecked()).toBeFalse();
        expect(repostButton.isEnabled()).toBeTrue();
    }

    @Test
    public void shouldUnsubscribeFromOngoingSubscriptionsWhenActivityDestroyed() {
        Track track = new Track();
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();

        final Subscription subscription = mock(Subscription.class);
        final Observable observable = mockObservable().howeverScheduled().returning(subscription).get();
        when(soundAssocOps.toggleLike(anyBoolean(), any(Playable.class))).thenReturn(observable);

        likeButton.performClick();

        controller.stopListeningForChanges();

        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldUpdateLikeOrRepostButtonWhenCurrentPlayableChanged() {
        Track track = new Track(1L);
        track.user_like = false;
        track.user_repost = false;
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();
        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        Track likedTrack = new Track(1L);
        likedTrack.user_like = true;
        likedTrack.user_repost = true;

        EventExpectation eventExpectation = EventExpectation.on(eventBus).withQueue(EventQueue.PLAYABLE_CHANGED);
        controller.startListeningForChanges();
        eventExpectation.verifyActionSubscribed();

        eventExpectation.publish(PlayableChangedEvent.create(likedTrack));
        expect(likeButton.isChecked()).toBeTrue();
        expect(repostButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldNotUpdateLikeOrRepostButtonStateForOtherPlayables() {
        Track track = new Track(1L);
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();
        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        Track likedTrack = new Track(2L);
        likedTrack.user_like = true;
        likedTrack.user_repost = true;

        EventExpectation eventExpectation = EventExpectation.on(eventBus).withQueue(EventQueue.PLAYABLE_CHANGED);
        controller.startListeningForChanges();
        eventExpectation.verifyActionSubscribed();

        eventExpectation.publish(PlayableChangedEvent.create(likedTrack));
        expect(likeButton.isChecked()).toBeFalse();
        expect(repostButton.isChecked()).toBeFalse();
    }

    @Test
    public void shouldGetContextFromOriginProvider() {
        OriginProvider originProvider = new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.PLAYER_MAIN.get();
            }
        };

        controller.setOriginProvider(originProvider);
        controller.setPlayable(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        EventBus.UI.subscribe(eventObserver);

        rootView.findViewById(R.id.btn_share).performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.PLAYER_MAIN.get());
    }

}
