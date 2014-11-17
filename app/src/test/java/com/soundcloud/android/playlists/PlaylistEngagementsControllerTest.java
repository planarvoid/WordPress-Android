package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
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
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ToggleButton;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistEngagementsControllerTest {

    private PlaylistEngagementsController controller;
    private ViewGroup rootView;
    private PublicApiTrack track;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private SoundAssociationOperations soundAssocOps;
    @Mock private Context context;
    @Mock private AccountOperations accountOperations;

    @Before
    public void setup() {
        LayoutInflater inflater = (LayoutInflater) Robolectric.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = (ViewGroup) inflater.inflate(R.layout.playlist_action_bar, null);
        controller = new PlaylistEngagementsController(eventBus, soundAssocOps, accountOperations);
        controller.bindView(rootView);
        controller.startListeningForChanges();
        track = createTrack();
    }

    @After
    public void tearDown() throws Exception {
        controller.stopListeningForChanges();
    }

    @Test
    public void shouldPublishUIEventWhenLikingPlayable() {
        controller.setPlayable(track);

        when(soundAssocOps.toggleLike(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.just(PropertySet.create()));
        rootView.findViewById(R.id.toggle_like).performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_LIKE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlayable() {
        controller.setPlayable(track);

        when(soundAssocOps.toggleLike(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.just(PropertySet.create()));
        ToggleButton likeToggle = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        likeToggle.setChecked(true);
        likeToggle.performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_UNLIKE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        controller.setPlayable(track);
        when(soundAssocOps.toggleRepost(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.just(PropertySet.create()));

        rootView.findViewById(R.id.toggle_repost).performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_REPOST);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        controller.setPlayable(track);
        when(soundAssocOps.toggleRepost(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.just(PropertySet.create()));
        ToggleButton repostToggle = (ToggleButton) rootView.findViewById(R.id.toggle_repost);

        repostToggle.setChecked(true);
        repostToggle.performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_UNREPOST);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        controller.setPlayable(track);

        rootView.findViewById(R.id.btn_share).performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_SHARE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        rootView.findViewById(R.id.btn_share).performClick();
        expect(eventBus.eventsOn(EventQueue.TRACKING)).toBeEmpty();
    }

    @Test
    public void shouldSendShareIntentWhenSharingPlayableWithEmptyUser() {
        controller.setPlayable(track);

        rootView.findViewById(R.id.btn_share).performClick();

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent).not.toBeNull();
        expect(shareIntent.getType()).toEqual("text/plain");
        expect(shareIntent.getAction()).toEqual(Intent.ACTION_SEND);
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("dubstep anthem - SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain("Listen to dubstep anthem #np on #SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain(track.permalink_url);
    }

    @Test
    public void shouldSendShareIntentWhenSharingPlayable() {
        track.user = new PublicApiUser();
        track.user.username  = "user";
        controller.setPlayable(track);

        rootView.findViewById(R.id.btn_share).performClick();

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("dubstep anthem - SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain("Listen to dubstep anthem by user #np on #SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain(track.permalink_url);
    }

    @Test
    public void shouldNotSendShareIntentWhenSharingPrivatePlayable() {
        track.sharing = Sharing.PRIVATE;
        controller.setPlayable(track);

        rootView.findViewById(R.id.btn_share).performClick();

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent).toBeNull();
    }

    @Test
    public void shouldLikeTrackWhenCheckingLikeButton() {
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();

        track.user_like = true;
        when(soundAssocOps.toggleLike(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.just(PropertySet.create()));

        likeButton.performClick();

        verify(soundAssocOps).toggleLike(eq(track.getUrn()), eq(true));
        expect(likeButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        controller.setPlayable(track);

        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        track.user_repost = true;
        when(soundAssocOps.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        repostButton.performClick();

        verify(soundAssocOps).toggleRepost(eq(track.getUrn()), eq(true));
        expect(repostButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldUnsubscribeFromOngoingSubscriptionsWhenActivityDestroyed() {
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();

        final Subscription likeSubscription = mock(Subscription.class);
        final Observable observable = TestObservables.fromSubscription(likeSubscription);
        when(soundAssocOps.toggleLike(any(Urn.class), anyBoolean())).thenReturn(observable);

        likeButton.performClick();

        controller.stopListeningForChanges();

        verify(likeSubscription).unsubscribe();
        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldBeAbleToUnsubscribeThenResubscribeToChangeEvents() {
        controller.setPlayable(track);

        controller.stopListeningForChanges();
        controller.startListeningForChanges();

        // make sure starting to listen again does not try to use a subscription that had already been closed
        // (in which case unsubscribe is called more than once)
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(track.getUrn(), true, track.likes_count));
        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldUpdateLikeOrRepostButtonWhenCurrentPlayableChanged() {
        track.user_like = false;
        track.user_repost = false;
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();
        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED,
                PlayableUpdatedEvent.forLike(track.getUrn(), true, track.likes_count));
        expect(likeButton.isChecked()).toBeTrue();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED,
                PlayableUpdatedEvent.forRepost(track.getUrn(), true, track.reposts_count));
        expect(repostButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldNotUpdateLikeOrRepostButtonStateForOtherPlayables() {
        controller.setPlayable(track);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();
        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(Urn.forTrack(2L), true, 1));
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forRepost(Urn.forTrack(2L), true, 1));
        expect(likeButton.isChecked()).toBeFalse();
        expect(repostButton.isChecked()).toBeFalse();
    }

    @Test
    public void shouldGetContextFromOriginProvider() {
        OriginProvider originProvider = new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.SEARCH_MAIN.get();
            }
        };

        controller.setOriginProvider(originProvider);
        controller.setPlayable(track);

        rootView.findViewById(R.id.btn_share).performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.SEARCH_MAIN.get());
    }

    private PublicApiTrack createTrack() {
        PublicApiTrack track = new PublicApiTrack(123L);
        track.sharing = Sharing.PUBLIC;
        track.title = "dubstep anthem";
        track.permalink_url = "http://soundcloud.com/foo/bar";
        return track;
    }

}
