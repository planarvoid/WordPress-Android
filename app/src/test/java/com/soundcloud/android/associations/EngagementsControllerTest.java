package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ToggleButton;

@RunWith(SoundCloudTestRunner.class)
public class EngagementsControllerTest {

    private EngagementsController controller;
    private ViewGroup rootView;
    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private SoundAssociationOperations soundAssocOps;
    @Mock
    private Context context;
    @Mock
    private AccountOperations accountOperations;

    @Before
    public void setup() {
        LayoutInflater inflater = (LayoutInflater) Robolectric.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = (ViewGroup) inflater.inflate(R.layout.player_action_bar, null);
        controller = new EngagementsController(eventBus, soundAssocOps, accountOperations);
        controller.bindView(rootView);
        controller.startListeningForChanges();
    }

    @After
    public void tearDown() throws Exception {
        controller.stopListeningForChanges();
    }

    @Test
    public void shouldPublishUIEventWhenLikingPlayable() {
        controller.setPlayable(new PublicApiTrack(1L));

        when(soundAssocOps.toggleLike(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(PropertySet.create()));
        rootView.findViewById(R.id.toggle_like).performClick();

        UIEvent uiEvent = eventBus.firstEventOn(EventQueue.UI);
        expect(uiEvent.getKind()).toBe(UIEvent.LIKE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlayable() {
        controller.setPlayable(new PublicApiTrack(1L));

        when(soundAssocOps.toggleLike(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(PropertySet.create()));
        ToggleButton likeToggle = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        likeToggle.setChecked(true);
        likeToggle.performClick();

        UIEvent uiEvent = eventBus.firstEventOn(EventQueue.UI);
        expect(uiEvent.getKind()).toBe(UIEvent.UNLIKE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        controller.setPlayable(new PublicApiTrack(1L));

        when(soundAssocOps.toggleRepost(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(new SoundAssociation(new PublicApiTrack())));
        rootView.findViewById(R.id.toggle_repost).performClick();

        UIEvent uiEvent = eventBus.firstEventOn(EventQueue.UI);
        expect(uiEvent.getKind()).toBe(UIEvent.REPOST);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        controller.setPlayable(new PublicApiTrack(1L));

        when(soundAssocOps.toggleRepost(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(new SoundAssociation(new PublicApiTrack())));
        ToggleButton repostToggle = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        repostToggle.setChecked(true);
        repostToggle.performClick();

        UIEvent uiEvent = eventBus.firstEventOn(EventQueue.UI);
        expect(uiEvent.getKind()).toBe(UIEvent.UNREPOST);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }


    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        controller.setPlayable(new PublicApiTrack(1L));

        rootView.findViewById(R.id.btn_share).performClick();

        UIEvent uiEvent = eventBus.firstEventOn(EventQueue.UI);
        expect(uiEvent.getKind()).toBe(UIEvent.SHARE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        rootView.findViewById(R.id.btn_share).performClick();
        expect(eventBus.eventsOn(EventQueue.UI)).toBeEmpty();
    }

    @Test
    public void shouldLikeTrackWhenCheckingLikeButton() {
        PublicApiTrack track = new PublicApiTrack();
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();

        track.user_like = true;
        when(soundAssocOps.toggleLike(anyBoolean(), any(Playable.class)))
                .thenReturn(Observable.just(PropertySet.create()));

        likeButton.performClick();

        verify(soundAssocOps).toggleLike(eq(true), refEq(track));
        expect(likeButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        PublicApiTrack track = new PublicApiTrack();
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
        PublicApiTrack track = new PublicApiTrack();
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
        PublicApiTrack track = new PublicApiTrack();
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();

        final Subscription likeSubscription = mock(Subscription.class);
        final Observable observable = TestObservables.fromSubscription(likeSubscription);
        when(soundAssocOps.toggleLike(anyBoolean(), any(Playable.class))).thenReturn(observable);

        likeButton.performClick();

        controller.stopListeningForChanges();

        verify(likeSubscription).unsubscribe();
        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldBeAbleToUnsubscribeThenResubscribeToChangeEvents() {
        final PublicApiTrack track = new PublicApiTrack(1L);
        controller.setPlayable(track);

        controller.stopListeningForChanges();
        controller.startListeningForChanges();

        // make sure starting to listen again does not try to use a subscription that had already been closed
        // (in which case unsubscribe is called more than once)
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.forLike(track.getUrn(), true, track.likes_count));
        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldUpdateLikeOrRepostButtonWhenCurrentPlayableChanged() {
        PublicApiTrack track = new PublicApiTrack(1L);
        track.user_like = false;
        track.user_repost = false;
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();
        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED,
                PlayableChangedEvent.forLike(track.getUrn(), true, track.likes_count));
        expect(likeButton.isChecked()).toBeTrue();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED,
                PlayableChangedEvent.forRepost(track.getUrn(), true, track.reposts_count));
        expect(repostButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldNotUpdateLikeOrRepostButtonStateForOtherPlayables() {
        PublicApiTrack track = new PublicApiTrack(1L);
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();
        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.forLike(Urn.forTrack(2L), true, 1));
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.forRepost(Urn.forTrack(2L), true, 1));
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
        controller.setPlayable(new PublicApiTrack(1L));

        rootView.findViewById(R.id.btn_share).performClick();

        UIEvent uiEvent = eventBus.firstEventOn(EventQueue.UI);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.PLAYER_MAIN.get());
    }

}
