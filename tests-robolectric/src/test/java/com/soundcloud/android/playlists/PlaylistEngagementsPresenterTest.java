package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playlists.PlaylistEngagementsView.OnEngagementListener;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.ViewGroup;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistEngagementsPresenterTest {

    private PlaylistEngagementsPresenter controller;
    private PlaylistInfo playlistInfo;
    private PublishSubject<Boolean> publishSubject;
    private TestEventBus eventBus;

    @Mock private RepostOperations repostOperations;
    @Mock private Context context;
    @Mock private AccountOperations accountOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private LikeOperations likeOperations;
    @Mock private PlaylistEngagementsView engagementsView;
    @Mock private ViewGroup rootView;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private Fragment fragment;
    @Mock private OfflinePlaybackOperations offlinePlaybackOperations;
    @Mock private PlaybackToastHelper playbackToastHelper;

    @Captor private ArgumentCaptor<OnEngagementListener> listenerCaptor;
    private OnEngagementListener onEngagementListener;
    private PublishSubject<DownloadState> playlistDownloadState;

    @Before
    public void setup() {
        eventBus = new TestEventBus();
        controller = new PlaylistEngagementsPresenter(eventBus,
                repostOperations, accountOperations,
                likeOperations, engagementsView,
                featureOperations, offlineContentOperations, offlinePlaybackOperations, playbackToastHelper);
        when(rootView.getContext()).thenReturn(Robolectric.application);
        playlistDownloadState = PublishSubject.create();
        when(offlineContentOperations.getPlaylistDownloadState(any(Urn.class))).thenReturn(playlistDownloadState);

        controller.bindView(rootView);
        controller.onResume(fragment);
        playlistInfo = createPublicPlaylistInfo();

        verify(engagementsView).setOnEngagement(listenerCaptor.capture());
        onEngagementListener = listenerCaptor.getValue();
        publishSubject = PublishSubject.create();
    }

    @After
    public void tearDown() {
        controller.onPause(fragment);
    }

    @Test
    public void shouldPublishUIEventWhenLikingAPlaylist() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        onEngagementListener.onToggleLike(true);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_LIKE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlaylist() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(likeOperations.toggleLike(playlistInfo.getUrn(), false)).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleLike(false);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_UNLIKE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleRepost(true, false);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_REPOST);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleRepost(false, false);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_UNREPOST);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());

        onEngagementListener.onShare();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_SHARE);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        onEngagementListener.onShare();
        expect(eventBus.eventsOn(EventQueue.TRACKING)).toBeEmpty();
    }

    @Test
    public void shouldSendShareIntentWhenSharingPlayable() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());

        onEngagementListener.onShare();

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual(playlistInfo.getTitle() + " - SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain("Listen to " + playlistInfo.getTitle() + " by " + playlistInfo.getCreatorName() + " #np on #SoundCloud\\n" + playlistInfo.getPermalinkUrl());
    }

    @Test
    public void shouldNotSendShareIntentWhenSharingPrivatePlaylist() {
        playlistInfo = createPlaylistInfoFromPrivatePlaylist();
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());

        onEngagementListener.onShare();

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent).toBeNull();
    }

    @Test
    public void shouldLikePlaylistWhenCheckingLikeButton() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(likeOperations.toggleLike(playlistInfo.getUrn(), true)).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleLike(true);

        verify(likeOperations).toggleLike(playlistInfo.getUrn(), true);
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleRepost(true, false);

        verify(repostOperations).toggleRepost(eq(playlistInfo.getUrn()), eq(true));
    }

    @Test
    public void shouldUnsubscribeFromOngoingSubscriptionsWhenActivityDestroyed() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());

        final Subscription likeSubscription = mock(Subscription.class);
        final Observable observable = TestObservables.fromSubscription(likeSubscription);
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(observable);

        onEngagementListener.onToggleLike(true);

        controller.onPause(fragment);

        verify(likeSubscription).unsubscribe();
        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldPlayShuffledThroughContentOperationsOnPlayShuffled() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        final PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(offlinePlaybackOperations.playPlaylistShuffled(playlistInfo.getUrn(), getPlaySessionSource()))
                .thenReturn(subject);

        onEngagementListener.onPlayShuffled();

        expect(subject.hasObservers()).toBeTrue();
    }

    @Test
    public void shouldBeAbleToUnsubscribeThenResubscribeToChangeEvents() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());

        controller.onPause(fragment);
        controller.onResume(fragment);

        // make sure starting to listen again does not try to use a subscription that had already been closed
        // (in which case unsubscribe is called more than once)
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(playlistInfo.getUrn(), true, playlistInfo.getLikesCount()));
        verify(engagementsView).updateLikeItem(playlistInfo.getLikesCount(), true);

    }

    @Test
    public void shouldUpdateLikeOrRepostButtonWhenCurrentPlayableChanged() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromLike(playlistInfo.getUrn(), true, playlistInfo.getLikesCount()));
        verify(engagementsView).updateLikeItem(playlistInfo.getLikesCount(), true);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromRepost(playlistInfo.getUrn(), true));
        verify(engagementsView).showPublicOptions(true);
    }

    @Test
    public void shouldNotUpdateLikeOrRepostButtonStateForOtherPlayables() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(2L), true, 1));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromRepost(Urn.forTrack(2L), true));

        verify(engagementsView, never()).updateLikeItem(anyInt(), eq(true));
        verify(engagementsView, never()).showPublicOptions(eq(true));
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnMarkedForOfflineChange() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(playlistInfo.getUrn(), true));

        verify(engagementsView).setOfflineOptionsMenu(true);
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnUnmarkedForOfflineChange() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(playlistInfo.getUrn(), false));

        verify(engagementsView).setOfflineOptionsMenu(false);
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
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());

        onEngagementListener.onShare();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getAttributes().get("context")).toEqual(Screen.SEARCH_MAIN.get());
    }

    @Test
    public void makeOfflineAvailableUsesOfflineOperationsToMakeOfflineAvailable() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistInfo.getUrn())).thenReturn(publishSubject);

        onEngagementListener.onMakeOfflineAvailable(true);

        expect(publishSubject.hasObservers()).toBeTrue();
    }

    @Test
    public void makeOfflineUnavailableUsesOfflineOperationsToMakeOfflineUnavailable() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        when(offlineContentOperations.makePlaylistUnavailableOffline(playlistInfo.getUrn())).thenReturn(publishSubject);

        onEngagementListener.onMakeOfflineAvailable(false);

        expect(publishSubject.hasObservers()).toBeTrue();
    }

    @Test
    public void showsOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedAvailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        controller.setPlaylistInfo(createPlaylistInfoWithOfflineAvailability(true), getPlaySessionSource());

        verify(engagementsView).setOfflineOptionsMenu(true);
    }

    @Test
    public void showsNotOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedUnavailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        controller.setPlaylistInfo(createPlaylistInfoWithOfflineAvailability(false), getPlaySessionSource());

        verify(engagementsView).setOfflineOptionsMenu(false);
    }

    @Test
    public void showsUpsellWhenOfflineContentIsNotEnabledAndAllowedToShowUpsell() {
        when(featureOperations.isOfflineContentUpsellEnabled()).thenReturn(true);

        controller.setPlaylistInfo(createPlaylistInfoWithOfflineAvailability(true), getPlaySessionSource());

        verify(engagementsView).showUpsell();
    }

    @Test
    public void hidesOfflineOptionsWhenOfflineContentIsNotEnabledAndNotAllowedToShowUpsell() {
        controller.setPlaylistInfo(createPlaylistInfoWithOfflineAvailability(true), getPlaySessionSource());

        verify(engagementsView).hideOfflineContentOptions();
    }

    @Test
    public void showDefaultDownloadStateWhenNoOffline() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        playlistDownloadState.onNext(DownloadState.NO_OFFLINE);

        verify(engagementsView).showDefaultState();
    }

    @Test
    public void showDefaultDownloadStateWhenRequested() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        playlistDownloadState.onNext(DownloadState.REQUESTED);

        verify(engagementsView).showDefaultState();
    }

    @Test
    public void setDownloadingStateWhenDownloading() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        playlistDownloadState.onNext(DownloadState.DOWNLOADING);

        verify(engagementsView).showDownloadingState();
    }

    @Test
    public void setDownloadedStateWhenDownloaded() {
        controller.setPlaylistInfo(playlistInfo, getPlaySessionSource());
        playlistDownloadState.onNext(DownloadState.DOWNLOADED);

        verify(engagementsView).showDownloadedState();
    }

    @Test
    public void disablesShuffleWithOneTrack() throws Exception {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC, false);
        sourceSet.put(PlaylistProperty.TRACK_COUNT, 1);
        List<PropertySet> tracks = CollectionUtils.toPropertySets(ModelFixtures.create(ApiTrack.class));

        controller.setPlaylistInfo(new PlaylistInfo(sourceSet, tracks), getPlaySessionSource());

        verify(engagementsView).disableShuffle();
    }

    @Test
    public void enablesShuffleWithMoreThanOneTrack() throws Exception {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC, false);
        List<PropertySet> tracks = CollectionUtils.toPropertySets(ModelFixtures.create(ApiTrack.class, 2));

        controller.setPlaylistInfo(new PlaylistInfo(sourceSet, tracks), getPlaySessionSource());

        verify(engagementsView).enableShuffle();
    }

    private PlaylistInfo createPublicPlaylistInfo() {
        return createPlaylistInfoWithSharing(Sharing.PUBLIC);
    }

    private PlaylistInfo createPlaylistInfoFromPrivatePlaylist() {
        return createPlaylistInfoWithSharing(Sharing.PRIVATE);
    }

    private PlaylistInfo createPlaylistInfoWithSharing(Sharing sharing) {
        final PropertySet sourceSet = createPlaylistProperties(sharing, false);
        List<PropertySet> tracks = CollectionUtils.toPropertySets(ModelFixtures.create(ApiTrack.class, 10));
        return new PlaylistInfo(sourceSet, tracks);
    }

    private PlaylistInfo createPlaylistInfoWithOfflineAvailability(boolean markedForOffline) {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC, markedForOffline);
        List<PropertySet> tracks = CollectionUtils.toPropertySets(ModelFixtures.create(ApiTrack.class, 10));
        return new PlaylistInfo(sourceSet, tracks);
    }

    private PropertySet createPlaylistProperties(Sharing sharing, boolean markedForOffline) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setSharing(sharing);
        final PropertySet sourceSet = playlist.toPropertySet();
        sourceSet.put(PlaylistProperty.IS_MARKED_FOR_OFFLINE, markedForOffline);
        sourceSet.put(PlaylistProperty.IS_LIKED, false);
        sourceSet.put(PlaylistProperty.IS_REPOSTED, false);
        return sourceSet;
    }

    private PlaySessionSource getPlaySessionSource() {
        return new PlaySessionSource(Screen.PLAYLIST_DETAILS);
    }
}
