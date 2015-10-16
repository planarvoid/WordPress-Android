package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.PlaylistEngagementsView.OnEngagementListener;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.ViewGroup;

public class PlaylistEngagementsPresenterTest extends AndroidUnitTest {

    private PlaylistEngagementsPresenter controller;
    private PlaylistWithTracks playlistWithTracks;
    private PublishSubject<Boolean> publishSubject;
    private TestEventBus eventBus;

    @Mock private RepostOperations repostOperations;
    @Mock private Context context;
    @Mock private AccountOperations accountOperations;
    @Mock private LikeOperations likeOperations;
    @Mock private PlaylistEngagementsView engagementsView;
    @Mock private ViewGroup rootView;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private Fragment fragment;
    @Mock private OfflinePlaybackOperations offlinePlaybackOperations;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private Navigator navigator;

    @Captor private ArgumentCaptor<OnEngagementListener> listenerCaptor;
    private OnEngagementListener onEngagementListener;

    @Before
    public void setup() {
        eventBus = new TestEventBus();
        controller = new PlaylistEngagementsPresenter(eventBus, repostOperations, accountOperations, likeOperations,
                engagementsView, featureOperations, offlineContentOperations, offlinePlaybackOperations,
                playbackToastHelper, navigator);
        when(rootView.getContext()).thenReturn(context);
        when(context.getResources()).thenReturn(resources());

        controller.bindView(rootView);
        controller.onResume(fragment);
        playlistWithTracks = createPlaylistInfoWithSharing(Sharing.PUBLIC);

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
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        onEngagementListener.onToggleLike(true);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getAttributes().get("context")).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlaylist() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(likeOperations.toggleLike(playlistWithTracks.getUrn(), false)).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleLike(false);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getAttributes().get("context")).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleRepost(true, false);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getAttributes().get("context")).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleRepost(false, false);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getAttributes().get("context")).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        onEngagementListener.onShare();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_SHARE);
        assertThat(uiEvent.getAttributes().get("context")).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        onEngagementListener.onShare();
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void shouldSendShareIntentWhenSharingPlayable() {
        String expectedTitle = "Title";
        String expectedShareText = "ShareText";

        when(context.getString(R.string.share_subject, playlistWithTracks.getTitle())).thenReturn(expectedTitle);
        when(context.getString(R.string.share_track_by_artist_on_soundcloud,
                playlistWithTracks.getTitle(), playlistWithTracks.getCreatorName(),
                playlistWithTracks.getPermalinkUrl())).thenReturn(expectedShareText);

        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        onEngagementListener.onShare();

        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(argumentCaptor.capture());

        Intent shareIntent = argumentCaptor.getValue();
        assertThat(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo(expectedTitle);
        assertThat(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).contains(expectedShareText);
    }

    @Test
    public void shouldNotSendShareIntentWhenSharingPrivatePlaylist() {
        playlistWithTracks = createPlaylistInfoWithSharing(Sharing.PRIVATE);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        onEngagementListener.onShare();

        verify(context, never()).startActivity(any(Intent.class));
    }

    @Test
    public void shouldLikePlaylistWhenCheckingLikeButton() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(likeOperations.toggleLike(playlistWithTracks.getUrn(), true)).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleLike(true);

        verify(likeOperations).toggleLike(playlistWithTracks.getUrn(), true);
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleRepost(true, false);

        verify(repostOperations).toggleRepost(eq(playlistWithTracks.getUrn()), eq(true));
    }

    @Test
    public void shouldUnsubscribeFromOngoingSubscriptionsWhenActivityDestroyed() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        controller.onPause(fragment);

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldPlayShuffledThroughContentOperationsOnPlayShuffled() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        final PublishSubject<PlaybackResult> subject = PublishSubject.create();
        when(offlinePlaybackOperations.playPlaylistShuffled(playlistWithTracks.getUrn(), getPlaySessionSource()))
                .thenReturn(subject);

        onEngagementListener.onPlayShuffled();

        assertThat(subject.hasObservers()).isTrue();
    }

    @Test
    public void shouldOpenUpgradeScreenWhenClickingOnUpsell() {
        controller.onUpsell(context);

        verify(navigator).openUpgrade(context);
    }

    @Test
    public void shouldBeAbleToUnsubscribeThenResubscribeToChangeEvents() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        controller.onPause(fragment);
        controller.onResume(fragment);

        // make sure starting to listen again does not try to use a subscription that had already been closed
        // (in which case unsubscribe is called more than once)
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(playlistWithTracks.getUrn(), true, playlistWithTracks.getLikesCount()));
        verify(engagementsView).updateLikeItem(playlistWithTracks.getLikesCount(), true);

    }

    @Test
    public void shouldUpdateLikeOrRepostButtonWhenCurrentPlayableChanged() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromLike(playlistWithTracks.getUrn(), true, playlistWithTracks.getLikesCount()));
        verify(engagementsView).updateLikeItem(playlistWithTracks.getLikesCount(), true);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromRepost(playlistWithTracks.getUrn(), true));
        verify(engagementsView).showPublicOptions(true);
    }

    @Test
    public void shouldNotUpdateLikeOrRepostButtonStateForOtherPlayables() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(2L), true, 1));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromRepost(Urn.forTrack(2L), true));

        verify(engagementsView, never()).updateLikeItem(anyInt(), eq(true));
        verify(engagementsView, never()).showPublicOptions(eq(true));
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnMarkedForOfflineChange() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(playlistWithTracks.getUrn(), true));

        verify(engagementsView).setOfflineOptionsMenu(true);
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnUnmarkedForOfflineChange() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(playlistWithTracks.getUrn(), false));

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
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        onEngagementListener.onShare();

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getAttributes().get("context")).isEqualTo(Screen.SEARCH_MAIN.get());
    }

    @Test
    public void makeOfflineAvailableUsesOfflineOperationsToMakeOfflineAvailable() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistWithTracks.getUrn())).thenReturn(publishSubject);

        onEngagementListener.onMakeOfflineAvailable(true);

        assertThat(publishSubject.hasObservers()).isTrue();
    }

    @Test
    public void makeOfflineUnavailableUsesOfflineOperationsToMakeOfflineUnavailable() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(offlineContentOperations.makePlaylistUnavailableOffline(playlistWithTracks.getUrn())).thenReturn(publishSubject);

        onEngagementListener.onMakeOfflineAvailable(false);

        assertThat(publishSubject.hasObservers()).isTrue();
    }

    @Test
    public void showsOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedAvailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, true)
                .put(PlaylistProperty.IS_POSTED, true);
        controller.setPlaylistInfo(createPlaylistWithTracks(sourceSet), getPlaySessionSource());

        verify(engagementsView).setOfflineOptionsMenu(true);
    }

    @Test
    public void showsNotOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedUnavailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_POSTED, true);
        controller.setPlaylistInfo(createPlaylistWithTracks(sourceSet), getPlaySessionSource());

        verify(engagementsView).setOfflineOptionsMenu(false);
    }

    @Test
    public void showsUpsellWhenOfflineContentIsNotEnabledAndAllowedToShowUpsell() {
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, true)
                .put(PlaylistProperty.IS_POSTED, true);
        controller.setPlaylistInfo(createPlaylistWithTracks(sourceSet), getPlaySessionSource());

        verify(engagementsView).showUpsell();
    }

    @Test
    public void hidesOfflineOptionsWhenOfflineContentIsNotEnabledAndNotAllowedToShowUpsell() {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, true)
                .put(PlaylistProperty.IS_POSTED, true);
        controller.setPlaylistInfo(createPlaylistWithTracks(sourceSet), getPlaySessionSource());

        verify(engagementsView).hideOfflineContentOptions();
    }

    @Test
    public void hidesOfflineOptionsWhenPlaylistIsNotPostedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC);
        controller.setPlaylistInfo(createPlaylistWithTracks(sourceSet), getPlaySessionSource());

        verify(engagementsView).hideOfflineContentOptions();
    }

    @Test
    public void doesNotHideOfflineOptionsWhenPlaylistIsLikedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_LIKED, true);
        controller.setPlaylistInfo(createPlaylistWithTracks(sourceSet), getPlaySessionSource());

        verify(engagementsView, never()).hideOfflineContentOptions();
    }

    @Test
    public void hidesOfflineOptionsWhenPlaylistIsNotLikedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC);
        controller.setPlaylistInfo(createPlaylistWithTracks(sourceSet), getPlaySessionSource());

        verify(engagementsView).hideOfflineContentOptions();
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsDownloadRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        reset(engagementsView);

        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloadRemoved(singletonList(playlistWithTracks.getUrn())));

        verify(engagementsView).showOfflineState(OfflineState.NO_OFFLINE);
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloadRequested(false, singletonList(playlistWithTracks.getUrn())));

        verify(engagementsView).showOfflineState(OfflineState.REQUESTED);
    }

    @Test
    public void setDownloadingStateWhenCurrentDownloadEmitsDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        final DownloadRequest request = ModelFixtures.downloadRequestFromPlaylists(track, true, singletonList(playlistWithTracks.getUrn()));

        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloading(request));

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADING);
    }

    @Test
    public void setDownloadedStateWhenCurrentDownloadEmitsDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloaded(false, singletonList(playlistWithTracks.getUrn())));

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void ignoreDownloadStateWhenCurrentDownloadEmitsAnUnrelatedEvent() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloaded(false, singletonList(Urn.forPlaylist(999999L))));

        verify(engagementsView, never()).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void showDefaultDownloadStateWhenPlaylistDownloadStateIsDownloadNoOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.NO_OFFLINE);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        verify(engagementsView).showOfflineState(OfflineState.NO_OFFLINE);
    }

    @Test
    public void showDefaultDownloadStateWhenPlaylistDownloadStateRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.REQUESTED);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        verify(engagementsView).showOfflineState(OfflineState.REQUESTED);
    }

    @Test
    public void setDownloadingStateWhenPlaylistDownloadStateDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADING);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADING);
    }

    @Test
    public void setDownloadedStateWhenPlaylistDownloadStateDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void disablesShuffleWithOneTrack() throws Exception {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_POSTED, true)
                .put(PlaylistProperty.TRACK_COUNT, 1);

        controller.setPlaylistInfo(createPlaylistWithSingleTrack(sourceSet), getPlaySessionSource());

        verify(engagementsView).disableShuffle();
    }

    @Test
    public void enablesShuffleWithMoreThanOneTrack() throws Exception {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_POSTED, true);

        controller.setPlaylistInfo(createPlaylistWithTracks(sourceSet), getPlaySessionSource());

        verify(engagementsView).enableShuffle();
    }

    private PlaylistWithTracks createPlaylistInfoWithSharing(Sharing sharing) {
        final PropertySet sourceSet = createPlaylistProperties(sharing)
                .put(PlaylistProperty.IS_POSTED, true);
        return createPlaylistWithTracks(sourceSet);
    }

    private PlaylistWithTracks createPlaylistWithTracks(PropertySet sourceSet) {
        return new PlaylistWithTracks(sourceSet, ModelFixtures.trackItems(10));
    }

    private PlaylistWithTracks createPlaylistWithSingleTrack(PropertySet sourceSet) {

        return new PlaylistWithTracks(sourceSet, ModelFixtures.trackItems(1));
    }

    private PropertySet createPlaylistProperties(Sharing sharing) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setSharing(sharing);
        return playlist.toPropertySet()
                .put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, false)
                .put(PlaylistProperty.IS_LIKED, false)
                .put(PlaylistProperty.IS_REPOSTED, false)
                .put(PlaylistProperty.IS_POSTED, false);
    }

    private PlaySessionSource getPlaySessionSource() {
        return new PlaySessionSource(Screen.PLAYLIST_DETAILS);
    }
}
