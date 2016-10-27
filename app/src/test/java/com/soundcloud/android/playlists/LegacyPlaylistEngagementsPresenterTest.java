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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
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
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.NetworkConnectionHelper;
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

public class LegacyPlaylistEngagementsPresenterTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123);

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.playlist_fragment, fragmentArgs());

    private LegacyPlaylistEngagementsPresenter presenter;
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
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private OfflineSettingsOperations offlineSettings;
    @Mock private ShareOperations shareOperations;
    @Mock private PlayQueueHelper playQueueHelper;
    @Mock private EventTracker eventTracker;

    @Captor private ArgumentCaptor<OnEngagementListener> listenerCaptor;
    @Captor private ArgumentCaptor<UIEvent> uiEventCaptor;
    private OnEngagementListener onEngagementListener;

    private static Bundle fragmentArgs() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(LegacyPlaylistDetailFragment.EXTRA_URN, PLAYLIST_URN);
        return bundle;
    }

    @Before
    public void setup() {
        eventBus = new TestEventBus();
        presenter = new LegacyPlaylistEngagementsPresenter(eventBus,
                                                           eventTracker,
                                                           repostOperations,
                                                           accountOperations,
                                                           likeOperations,
                                                           engagementsView,
                                                           featureOperations,
                                                           offlineContentOperations,
                                                           playbackInitiator,
                                                           playlistOperations,
                                                           playbackToastHelper,
                                                           connectionHelper,
                                                           offlineSettings,
                                                           navigator,
                                                           shareOperations,
                                                           playQueueHelper);

        presenter.bindView(fragmentRule.getView());
        presenter.onResume(fragmentRule.getFragment());
        playlistWithTracks = createPlaylistInfoWithSharing(Sharing.PUBLIC);

        verify(engagementsView).setOnEngagementListener(listenerCaptor.capture());
        onEngagementListener = listenerCaptor.getValue();
        publishSubject = PublishSubject.create();

        when(playlistOperations.trackUrnsForPlayback(playlistWithTracks.getUrn())).thenReturn(playlistTrackurns);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
    }

    @After
    public void tearDown() {
        presenter.onPause(fragmentRule.getFragment());
    }

    @Test
    public void shouldPublishUIEventWhenLikingAPlaylist() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());
        onEngagementListener.onToggleLike(true);

        UIEvent uiEvent = uiEventCaptor.getValue();
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getContextScreen()).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlaylist() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(likeOperations.toggleLike(playlistWithTracks.getUrn(),
                                       false)).thenReturn(Observable.just(PropertySet.create()));
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());

        onEngagementListener.onToggleLike(false);

        UIEvent uiEvent = uiEventCaptor.getValue();
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getContextScreen()).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(repostOperations.toggleRepost(any(Urn.class),
                                           anyBoolean())).thenReturn(Observable.just(PropertySet.create()));
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());

        onEngagementListener.onToggleRepost(true, false);

        UIEvent uiEvent = uiEventCaptor.getValue();
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(repostOperations.toggleRepost(any(Urn.class),
                                           anyBoolean())).thenReturn(Observable.just(PropertySet.create()));
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());

        onEngagementListener.onToggleRepost(false, false);

        UIEvent uiEvent = uiEventCaptor.getValue();
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo(Screen.UNKNOWN.get());
    }

    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        onEngagementListener.onShare();
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                        .contextScreen(Screen.UNKNOWN.get())
                                                                        .pageName(Screen.PLAYLIST_DETAILS.get())
                                                                        .pageUrn(playlistWithTracks.getUrn())
                                                                        .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                                                        .build();
        verify(shareOperations).share(fragmentRule.getActivity(),
                                      playlistWithTracks.getSourceSet(),
                                      eventContextMetadata,
                                      null);
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
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(likeOperations.toggleLike(playlistWithTracks.getUrn(),
                                       true)).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleLike(true);

        verify(likeOperations).toggleLike(playlistWithTracks.getUrn(), true);
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(repostOperations.toggleRepost(any(Urn.class),
                                           anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        onEngagementListener.onToggleRepost(true, false);

        verify(repostOperations).toggleRepost(eq(playlistWithTracks.getUrn()), eq(true));
    }

    @Test
    public void shouldUnsubscribeFromOngoingSubscriptionsWhenActivityDestroyed() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        presenter.onPause(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldPlayShuffledThroughContentOperationsOnPlayShuffled() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        final PublishSubject<PlaybackResult> subject = PublishSubject.create();
        when(playbackInitiator.playTracksShuffled(playlistTrackurns,
                                                  getPlaySessionSource()))
                .thenReturn(subject);

        onEngagementListener.onPlayShuffled();

        assertThat(subject.hasObservers()).isTrue();
    }

    @Test
    public void shouldOpenUpgradeScreenWhenClickingOnUpsell() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        presenter.onUpsell(fragmentRule.getActivity());

        verify(navigator).openUpgrade(fragmentRule.getActivity());
    }

    @Test
    public void shouldBeAbleToUnsubscribeThenResubscribeToChangeEvents() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        presenter.onPause(fragmentRule.getFragment());
        presenter.onResume(fragmentRule.getFragment());

        // make sure starting to listen again does not try to use a subscription that had already been closed
        // (in which case unsubscribe is called more than once)
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromLike(playlistWithTracks.getUrn(),
                                                          true,
                                                          playlistWithTracks.getLikesCount()));
        verify(engagementsView).updateLikeItem(playlistWithTracks.getLikesCount(), true);

    }

    @Test
    public void shouldShowUsersOptions() {
        when(accountOperations.isLoggedInUser(playlistWithTracks.getCreatorUrn())).thenReturn(true);

        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        verify(engagementsView).showMyOptions();
    }

    @Test
    public void shouldHideUsersOptions() {
        when(accountOperations.isLoggedInUser(playlistWithTracks.getCreatorUrn())).thenReturn(false);

        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        verify(engagementsView).hideMyOptions();
    }

    @Test
    public void shouldUpdateLikeOrRepostButtonWhenCurrentPlayableChanged() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromLike(playlistWithTracks.getUrn(),
                                                          true,
                                                          playlistWithTracks.getLikesCount()));
        verify(engagementsView).updateLikeItem(playlistWithTracks.getLikesCount(), true);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromRepost(playlistWithTracks.getUrn(), true));
        verify(engagementsView).showPublicOptions(true);
    }

    @Test
    public void shouldNotUpdateLikeOrRepostButtonStateForOtherPlayables() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(2L), true, 1));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromRepost(Urn.forTrack(2L), true));

        verify(engagementsView, never()).updateLikeItem(anyInt(), eq(true));
        verify(engagementsView, never()).showPublicOptions(eq(true));
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnMarkedForOfflineChange() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).showMakeAvailableOfflineButton(true);
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnUnmarkedForOfflineChange() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(playlistWithTracks.getUrn()));

        verify(engagementsView).showMakeAvailableOfflineButton(false);
    }

    @Test
    public void shouldGetContextFromOriginProvider() {
        OriginProvider originProvider = new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.SEARCH_MAIN.get();
            }
        };

        presenter.setOriginProvider(originProvider);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        onEngagementListener.onShare();
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                        .contextScreen(Screen.SEARCH_MAIN.get())
                                                                        .pageName(Screen.PLAYLIST_DETAILS.get())
                                                                        .pageUrn(playlistWithTracks.getUrn())
                                                                        .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                                                        .build();
        verify(shareOperations).share(fragmentRule.getActivity(),
                                      playlistWithTracks.getSourceSet(),
                                      eventContextMetadata,
                                      null);
    }

    @Test
    public void makeOfflineAvailableUsesOfflineOperationsToMakeOfflineAvailable() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistWithTracks.getUrn())).thenReturn(
                publishSubject);

        onEngagementListener.onMakeOfflineAvailable(true);

        assertThat(publishSubject.hasObservers()).isTrue();
    }

    @Test
    public void makeOfflineUnavailableUsesOfflineOperationsToMakeOfflineUnavailable() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(offlineContentOperations.makePlaylistUnavailableOffline(playlistWithTracks.getUrn())).thenReturn(
                publishSubject);

        onEngagementListener.onMakeOfflineAvailable(false);

        assertThat(publishSubject.hasObservers()).isTrue();
    }

    @Test
    public void showsOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedAvailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(OfflineProperty.IS_MARKED_FOR_OFFLINE, true)
                .put(PlaylistProperty.IS_POSTED, true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithTracks(sourceSet),
                                                            getPlaySessionSource()));

        verify(engagementsView).showMakeAvailableOfflineButton(true);
    }

    @Test
    public void showsNotOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedUnavailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_POSTED, true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithTracks(sourceSet),
                                                            getPlaySessionSource()));

        verify(engagementsView).showMakeAvailableOfflineButton(false);
    }

    @Test
    public void showsUpsellWhenOfflineContentIsNotEnabledAndAllowedToShowUpsell() {
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(OfflineProperty.IS_MARKED_FOR_OFFLINE, true)
                .put(PlaylistProperty.IS_POSTED, true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithTracks(sourceSet),
                                                            getPlaySessionSource()));

        verify(engagementsView).showUpsell();
    }

    @Test
    public void hidesOfflineOptionsWhenOfflineContentIsNotEnabledAndNotAllowedToShowUpsell() {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(OfflineProperty.IS_MARKED_FOR_OFFLINE, true)
                .put(PlaylistProperty.IS_POSTED, true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithTracks(sourceSet),
                                                            getPlaySessionSource()));

        verify(engagementsView).hideMakeAvailableOfflineButton();
    }

    @Test
    public void hidesOfflineOptionsWhenPlaylistIsNotPostedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithTracks(sourceSet),
                                                            getPlaySessionSource()));

        verify(engagementsView).hideMakeAvailableOfflineButton();
    }

    @Test
    public void showsOfflineOptionsWhenPlaylistIsLikedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_USER_LIKE, true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithTracks(sourceSet),
                                                            getPlaySessionSource()));

        verify(engagementsView).showMakeAvailableOfflineButton(false);
    }

    @Test
    public void hidesOfflineOptionsWhenPlaylistIsNotLikedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithTracks(sourceSet),
                                                            getPlaySessionSource()));

        verify(engagementsView).hideMakeAvailableOfflineButton();
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsDownloadRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(playlistWithTracks.getUrn()));

        verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).showOfflineState(OfflineState.REQUESTED);
    }

    @Test
    public void setDownloadingStateWhenCurrentDownloadEmitsDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        final ApiTrack track = ModelFixtures.create(ApiTrack.class);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloading(Arrays.asList(track.getUrn(), playlistWithTracks.getUrn()), true));

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADING);
    }

    @Test
    public void setDownloadedStateWhenCurrentDownloadEmitsDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded(singletonList(playlistWithTracks.getUrn()), false));

        final InOrder inOrder = inOrder(engagementsView);
        inOrder.verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
        inOrder.verify(engagementsView).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void ignoreDownloadStateWhenCurrentDownloadEmitsAnUnrelatedEvent() {
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded(singletonList(Urn.forPlaylist(12344443212L)), false));

        verify(engagementsView, never()).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void showDefaultDownloadStateWhenPlaylistDownloadStateIsDownloadNoOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void showDefaultDownloadStateWhenPlaylistDownloadStateRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.REQUESTED);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        verify(engagementsView).showOfflineState(OfflineState.REQUESTED);
    }

    @Test
    public void setDownloadingStateWhenPlaylistDownloadStateDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADING);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADING);
    }

    @Test
    public void setDownloadedStateWhenPlaylistDownloadStateDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void showWarningTextWhenPendingDownloadAndOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).showNoConnection();
    }

    @Test
    public void showWarningTextWhenPendingDownloadAndWifiOnly() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).showNoWifi();
    }

    @Test
    public void doNotShowWarningTextForNonPendingStates() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        presenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView, times(0)).showNoConnection();
    }

    @Test
    public void disablesShuffleWithOneTrack() throws Exception {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_POSTED, true)
                .put(PlaylistProperty.TRACK_COUNT, 1);

        presenter.setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithSingleTrack(sourceSet),
                                                            getPlaySessionSource()));

        verify(engagementsView).disableShuffle();
    }

    @Test
    public void enablesShuffleWithMoreThanOneTrack() throws Exception {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_POSTED, true);

        presenter.setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithTracks(sourceSet),
                                                            getPlaySessionSource()));

        verify(engagementsView).enableShuffle();
    }

    @Test
    public void shouldTrackUpsellImpressionInOnCreateWhenFeatureAvailable() {
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        presenter.onCreate(fragmentRule.getFragment(), null);

        UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(event.get(UpgradeFunnelEvent.KEY_PAGE_URN)).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void shouldNotTrackUpsellImpressionInOnCreateWhenFeatureNotAvailable() {
        when(featureOperations.upsellOfflineContent()).thenReturn(false);

        presenter.onCreate(fragmentRule.getFragment(), null);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void shouldPlayNext() {
        presenter.onPlayNext(Urn.NOT_SET);

        verify(playQueueHelper, times(1)).playNext(eq(Urn.NOT_SET));
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
                       .put(OfflineProperty.IS_MARKED_FOR_OFFLINE, false)
                       .put(PlaylistProperty.IS_USER_LIKE, false)
                       .put(PlaylistProperty.IS_USER_REPOST, false)
                       .put(PlaylistProperty.IS_POSTED, false);
    }

    private PlaySessionSource getPlaySessionSource() {
        return new PlaySessionSource(Screen.PLAYLIST_DETAILS);
    }
}
