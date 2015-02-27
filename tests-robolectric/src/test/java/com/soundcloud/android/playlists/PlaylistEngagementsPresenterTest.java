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
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.associations.LegacyRepostOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.CollectionUtils;
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

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistEngagementsPresenterTest {

    private PlaylistEngagementsPresenter controller;
    private ViewGroup rootView;
    private PlaylistInfo playlistInfo;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private LegacyRepostOperations soundAssocOps;
    @Mock private Context context;
    @Mock private AccountOperations accountOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private LikeOperations likeOperations;
    @Mock private LegacyPlaylistOperations legacyPlaylistOperations;

    @Before
    public void setup() {
        LayoutInflater inflater = (LayoutInflater) Robolectric.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = (ViewGroup) inflater.inflate(R.layout.playlist_action_bar, null);
        controller = new PlaylistEngagementsPresenter(eventBus, soundAssocOps, accountOperations, likeOperations);
        controller.bindView(rootView);
        controller.startListeningForChanges();
        playlistInfo = createPublicPlaylistInfo();
    }

    @After
    public void tearDown() throws Exception {
        controller.stopListeningForChanges();
    }

    @Test
    public void shouldPublishUIEventWhenLikingAPlaylist() {
        controller.setPlaylistInfo(playlistInfo);
        when(likeOperations.addLike(any(PropertySet.class))).thenReturn(Observable.just(PropertySet.create()));

        rootView.findViewById(R.id.toggle_like).performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_LIKE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlaylist() {
        controller.setPlaylistInfo(playlistInfo);
        when(likeOperations.removeLike(any(PropertySet.class))).thenReturn(Observable.just(PropertySet.create()));

        ToggleButton likeToggle = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        likeToggle.setChecked(true);
        likeToggle.performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_UNLIKE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        controller.setPlaylistInfo(playlistInfo);
        when(soundAssocOps.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        rootView.findViewById(R.id.toggle_repost).performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_REPOST);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        controller.setPlaylistInfo(playlistInfo);
        when(soundAssocOps.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        ToggleButton repostToggle = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        repostToggle.setChecked(true);
        repostToggle.performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_UNREPOST);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        controller.setPlaylistInfo(playlistInfo);

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
    public void shouldSendShareIntentWhenSharingPlayable() {
        controller.setPlaylistInfo(playlistInfo);

        rootView.findViewById(R.id.btn_share).performClick();

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual(playlistInfo.getTitle() + " - SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain("Listen to " + playlistInfo.getTitle() +" by " + playlistInfo.getCreatorName() +" #np on #SoundCloud\\n" + playlistInfo.getPermalinkUrl());
    }

    @Test
    public void shouldNotSendShareIntentWhenSharingPrivatePlaylist() {
        playlistInfo = createPlaylistInfoFromPrivatePlaylist();
        controller.setPlaylistInfo(playlistInfo);

        rootView.findViewById(R.id.btn_share).performClick();

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent).toBeNull();
    }

    @Test
    public void shouldLikePlaylistWhenCheckingLikeButton() {
        controller.setPlaylistInfo(playlistInfo);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();

        when(likeOperations.addLike(any(PropertySet.class))).thenReturn(Observable.just(PropertySet.create()));

        likeButton.performClick();

        verify(likeOperations).addLike(eq(playlistInfo.getSourceSet()));
        expect(likeButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        controller.setPlaylistInfo(playlistInfo);

        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        when(soundAssocOps.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        repostButton.performClick();

        verify(soundAssocOps).toggleRepost(eq(playlistInfo.getUrn()), eq(true));
        expect(repostButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldUnsubscribeFromOngoingSubscriptionsWhenActivityDestroyed() {
        controller.setPlaylistInfo(playlistInfo);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();

        final Subscription likeSubscription = mock(Subscription.class);
        final Observable observable = TestObservables.fromSubscription(likeSubscription);
        when(likeOperations.addLike(any(PropertySet.class))).thenReturn(observable);

        likeButton.performClick();

        controller.stopListeningForChanges();

        verify(likeSubscription).unsubscribe();
        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldBeAbleToUnsubscribeThenResubscribeToChangeEvents() {
        controller.setPlaylistInfo(playlistInfo);

        controller.stopListeningForChanges();
        controller.startListeningForChanges();

        // make sure starting to listen again does not try to use a subscription that had already been closed
        // (in which case unsubscribe is called more than once)
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(playlistInfo.getUrn(), true, playlistInfo.getLikesCount()));
        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldUpdateLikeOrRepostButtonWhenCurrentPlayableChanged() {
        controller.setPlaylistInfo(playlistInfo);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();
        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromLike(playlistInfo.getUrn(), true, playlistInfo.getLikesCount()));
        expect(likeButton.isChecked()).toBeTrue();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromRepost(playlistInfo.getUrn(), true, playlistInfo.getRepostsCount()));
        expect(repostButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldNotUpdateLikeOrRepostButtonStateForOtherPlayables() {
        controller.setPlaylistInfo(playlistInfo);

        ToggleButton likeButton = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        expect(likeButton.isChecked()).toBeFalse();
        ToggleButton repostButton = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        expect(repostButton.isChecked()).toBeFalse();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(2L), true, 1));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromRepost(Urn.forTrack(2L), true, 1));
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
        controller.setPlaylistInfo(playlistInfo);

        rootView.findViewById(R.id.btn_share).performClick();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.SEARCH_MAIN.get());
    }

    private PlaylistInfo createPublicPlaylistInfo() {
        return createPlaylistInfoWithSharing(Sharing.PUBLIC);
    }

    private PlaylistInfo createPlaylistInfoFromPrivatePlaylist() {
        return createPlaylistInfoWithSharing(Sharing.PRIVATE);
    }

    private PlaylistInfo createPlaylistInfoWithSharing(Sharing sharing) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setSharing(sharing);
        final PropertySet sourceSet = playlist.toPropertySet();
        sourceSet.put(PlaylistProperty.IS_LIKED, false);
        sourceSet.put(PlaylistProperty.IS_REPOSTED, false);
        List<PropertySet> tracks = CollectionUtils.toPropertySets(ModelFixtures.create(ApiTrack.class, 10));
        return new PlaylistInfo(sourceSet, tracks);
    }
}
