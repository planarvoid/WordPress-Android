package com.soundcloud.android.playlists;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.removed;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static com.soundcloud.android.playlists.PlaylistEngagementsView.OnEngagementListener;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.os.Bundle;

import java.util.Arrays;
import java.util.List;

public class PlaylistEngagementsPresenterTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123);

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.playlist_fragment, fragmentArgs());

    private PlaylistEngagementsPresenter controller;
    private PlaylistWithTracks playlistWithTracks;
    private PublishSubject<Void> publishSubject;
    private TestEventBus eventBus;
    private Observable<List<Urn>> playlistTrackurns = Observable.just(Arrays.asList(Urn.forTrack(1)));

    @Mock private RepostOperations repostOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private LikeOperations likeOperations;
    @Mock private PlaylistEngagementsView engagementsView;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private Navigator navigator;
    @Mock private ShareOperations shareOperations;

    @Captor private ArgumentCaptor<OnEngagementListener> listenerCaptor;
    private OnEngagementListener onEngagementListener;

    private static Bundle fragmentArgs() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(PlaylistDetailFragment.EXTRA_URN, PLAYLIST_URN);
        return bundle;
    }

    @Before
    public void setup() {
        eventBus = new TestEventBus();
        controller = new PlaylistEngagementsPresenter(eventBus, repostOperations, accountOperations, likeOperations,
                engagementsView, featureOperations, offlineContentOperations, playbackInitiator,
                playlistOperations, playbackToastHelper, navigator, shareOperations);

        controller.bindView(fragmentRule.getView());
        controller.onResume(fragmentRule.getFragment());
        playlistWithTracks = createPlaylistInfoWithSharing(Sharing.PUBLIC);

        verify(engagementsView).setOnEngagement(listenerCaptor.capture());
        onEngagementListener = listenerCaptor.getValue();
        publishSubject = PublishSubject.create();

        when(playlistOperations.trackUrnsForPlayback(playlistWithTracks.getUrn())).thenReturn(playlistTrackurns);
    }

    @After
    public void tearDown() {
        controller.onPause(fragmentRule.getFragment());
    }

    @Test
    public void shouldPublishUIEventWhenLikingAPlaylist() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        onEngagementListener.onToggleLike(true);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getContextScreen()).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlaylist() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(likeOperations.toggleLike(playlistWithTracks.getUrn(), false)).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleLike(false);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getContextScreen()).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleRepost(true, false);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleRepost(false, false);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        onEngagementListener.onShare();
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                .contextScreen(Screen.UNKNOWN.get())
                .pageName(Screen.PLAYLIST_DETAILS.get())
                .pageUrn(playlistWithTracks.getUrn())
                .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                .build();
        verify(shareOperations).share(fragmentRule.getActivity(), playlistWithTracks.getSourceSet(), eventContextMetadata, null);
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        onEngagementListener.onShare();

        verify(shareOperations, never())
                .share(any(Context.class), any(PropertySet.class),
                        any(EventContextMetadata.class), any(PromotedSourceInfo.class));
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

        controller.onPause(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldPlayShuffledThroughContentOperationsOnPlayShuffled() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        final PublishSubject<PlaybackResult> subject = PublishSubject.create();
        when(playbackInitiator.playTracksShuffled(playlistTrackurns, getPlaySessionSource()))
                .thenReturn(subject);

        onEngagementListener.onPlayShuffled();

        assertThat(subject.hasObservers()).isTrue();
    }

    @Test
    public void shouldOpenUpgradeScreenWhenClickingOnUpsell() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        controller.onUpsell(fragmentRule.getActivity());

        verify(navigator).openUpgrade(fragmentRule.getActivity());
    }

    @Test
    public void shouldBeAbleToUnsubscribeThenResubscribeToChangeEvents() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        controller.onPause(fragmentRule.getFragment());
        controller.onResume(fragmentRule.getFragment());

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

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).setOfflineOptionsMenu(true);
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnUnmarkedForOfflineChange() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(playlistWithTracks.getUrn()));

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
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                .contextScreen(Screen.SEARCH_MAIN.get())
                .pageName(Screen.PLAYLIST_DETAILS.get())
                .pageUrn(playlistWithTracks.getUrn())
                .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                .build();
        verify(shareOperations).share(fragmentRule.getActivity(), playlistWithTracks.getSourceSet(), eventContextMetadata, null);
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
                .put(PlaylistProperty.IS_USER_LIKE, true);
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

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(playlistWithTracks.getUrn()));

        verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).showOfflineState(OfflineState.REQUESTED);
    }

    @Test
    public void setDownloadingStateWhenCurrentDownloadEmitsDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        final ApiTrack track = ModelFixtures.create(ApiTrack.class);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloading(Arrays.asList(track.getUrn(), playlistWithTracks.getUrn()), true));

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADING);
    }

    @Test
    public void setDownloadedStateWhenCurrentDownloadEmitsDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                downloaded(singletonList(playlistWithTracks.getUrn()), false));

        final InOrder inOrder = inOrder(engagementsView);
        inOrder.verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
        inOrder.verify(engagementsView).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void ignoreDownloadStateWhenCurrentDownloadEmitsAnUnrelatedEvent() {
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloaded(singletonList(Urn.forPlaylist(12344443212L)), false));

        verify(engagementsView, never()).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void showDefaultDownloadStateWhenPlaylistDownloadStateIsDownloadNoOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
        controller.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
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

    @Test
    public void shouldTrackUpsellImpressionInOnCreateWhenFeatureAvailable() {
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        controller.onCreate(fragmentRule.getFragment(), null);

        UpgradeTrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeTrackingEvent.class);
        assertThat(event.get(UpgradeTrackingEvent.KEY_PAGE_URN)).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void shouldNotTrackUpsellImpressionInOnCreateWhenFeatureNotAvailable() {
        when(featureOperations.upsellOfflineContent()).thenReturn(false);

        controller.onCreate(fragmentRule.getFragment(), null);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
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
                .put(PlaylistProperty.IS_USER_LIKE, false)
                .put(PlaylistProperty.IS_USER_REPOST, false)
                .put(PlaylistProperty.IS_POSTED, false);
    }

    private PlaySessionSource getPlaySessionSource() {
        return new PlaySessionSource(Screen.PLAYLIST_DETAILS);
    }
}
