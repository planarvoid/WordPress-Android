package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.annotations.Issue;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

public class PlaylistItemMenuPresenterTest extends AndroidUnitTest {

    public static final String SCREEN = "some tag";
    @Mock private Context context;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private LikeOperations likeOperations;
    @Mock private RepostOperations repostOperations;
    @Mock private SharePresenter sharePresenter;
    @Mock private ScreenProvider screenProvider;
    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private Navigator navigator;
    @Mock private MenuItem menuItem;
    @Mock private FeatureFlags featureFlags;
    @Mock private PlayQueueHelper playQueueHelper;
    @Mock private EventTracker eventTracker;
    @Mock private PlaylistItemMenuRendererFactory playlistMenuRenderFactory;
    @Mock private PlaylistItemMenuRenderer playlistMenuRenderer;

    @Captor private ArgumentCaptor<UIEvent> uiEventArgumentCaptor;

    private View button;

    private final TestEventBus eventBus = new TestEventBus();

    private PlaylistItemMenuPresenter presenter;
    private final Playlist playlist = ModelFixtures.playlist();
    private PlaylistItem playlistItem = PlaylistItem.from(playlist);

    @Before
    public void setUp() throws Exception {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.empty());
        when(playlistOperations.trackUrnsForPlayback(any(Urn.class)))
                .thenReturn(Observable.<List<Urn>>just(Lists.newArrayList(Urn.NOT_SET)));


        when(offlineOperations.makePlaylistAvailableOffline(any(Urn.class))).thenReturn(Observable.empty());
        when(offlineOperations.makePlaylistUnavailableOffline(any(Urn.class))).thenReturn(Observable.empty());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        when(likeOperations.toggleLike(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.empty());

        when(screenProvider.getLastScreenTag()).thenReturn(SCREEN);
        presenter = new PlaylistItemMenuPresenter(context,
                                                  eventBus,
                                                  playlistOperations,
                                                  likeOperations,
                                                  repostOperations,
                                                  sharePresenter,
                                                  screenProvider,
                                                  featureOperations,
                                                  offlineOperations,
                                                  navigator,
                                                  playQueueHelper,
                                                  eventTracker,
                                                  playlistMenuRenderFactory, accountOperations);

        button = new View(activity());

        when(playlistMenuRenderFactory.create(presenter, button)).thenReturn(playlistMenuRenderer);
    }

    @Test
    public void clickingOnUpsellItemNavigatesToUpgrade() {
        when(menuItem.getItemId()).thenReturn(R.id.upsell_offline_content);

        presenter.show(button, playlistItem);
        presenter.handleUpsell(context);

        verify(navigator).openUpgrade(context);
    }

    @Test
    public void clickingOnAddToLikesAddPlaylistLike() {
        final PublishSubject<LikeOperations.LikeResult> likeObservable = PublishSubject.create();
        when(likeOperations.toggleLike(playlistItem.getUrn(), !playlistItem.isUserLike())).thenReturn(likeObservable);
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.show(button, playlistItem);
        presenter.handleLike(playlistItem);

        assertThat(likeObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickRepostItemRepostsPlaylist() {
        final PublishSubject<RepostOperations.RepostResult> repostObservable = PublishSubject.create();
        when(repostOperations.toggleRepost(playlistItem.getUrn(), !playlistItem.isUserRepost())).thenReturn(
                repostObservable);
        when(menuItem.getItemId()).thenReturn(R.id.toggle_repost);

        presenter.show(button, playlistItem);
        presenter.handleRepost(!playlistItem.isUserRepost());

        assertThat(repostObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickingOnRepostSendsTrackingEvent() {
        final PublishSubject<RepostOperations.RepostResult> repostObservable = PublishSubject.create();
        when(repostOperations.toggleRepost(playlistItem.getUrn(), !playlistItem.isUserRepost())).thenReturn(
                repostObservable);
        when(menuItem.getItemId()).thenReturn(R.id.toggle_repost);

        presenter.show(button, playlistItem);
        presenter.handleRepost(!playlistItem.isUserRepost());

        verify(eventTracker).trackEngagement(uiEventArgumentCaptor.capture());

        UIEvent uiEvent = uiEventArgumentCaptor.getValue();
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.isFromOverflow().get()).isTrue();
    }

    @Test
    public void clickingOnShareItemSharesPlaylist() {
        when(menuItem.getItemId()).thenReturn(R.id.share);

        presenter.show(button, playlistItem);
        presenter.handleShare(context, playlistItem);

        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                        .contextScreen(screenProvider.getLastScreenTag())
                                                                        .pageName(screenProvider.getLastScreenTag())
                                                                        .isFromOverflow(true)
                                                                        .invokerScreen(ScreenElement.LIST.get())
                                                                        .build();

        verify(sharePresenter).share(context,
                                     playlistItem.permalinkUrl(),
                                     eventContextMetadata, null,
                                     EntityMetadata.from(playlistItem));
    }

    @Test
    public void clickingOnAddToLikesSendsTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.show(button, playlistItem);
        presenter.handleLike(playlistItem);

        verify(eventTracker).trackEngagement(uiEventArgumentCaptor.capture());

        UIEvent uiEvent = uiEventArgumentCaptor.getValue();
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.isFromOverflow().get()).isTrue();
    }

    @Test
    public void shouldSaveOfflineIfPlaylistOwnedByCurrentUser() {
        final PublishSubject<Void> offlineObservable = PublishSubject.create();
        when(accountOperations.isLoggedInUser(playlistItem.creatorUrn())).thenReturn(true);
        when(offlineOperations.makePlaylistAvailableOffline(playlistItem.getUrn())).thenReturn(offlineObservable);
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_available);

        presenter.show(button, playlistItem);
        presenter.saveOffline(playlistItem);

        assertThat(offlineObservable.hasObservers()).isTrue();
        verifyZeroInteractions(likeOperations);
    }

    @Test
    public void shouldSaveOfflineIfPlaylistLikedByCurrentUser() {
        final PublishSubject<Void> offlineObservable = PublishSubject.create();
        final PlaylistItem likedPlaylist = playlistItem.updateLikeState(true);
        when(offlineOperations.makePlaylistAvailableOffline(likedPlaylist.getUrn())).thenReturn(offlineObservable);
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_available);

        presenter.show(button, likedPlaylist);
        presenter.saveOffline(likedPlaylist);

        assertThat(offlineObservable.hasObservers()).isTrue();
        verifyZeroInteractions(likeOperations);
    }

    @Test
    public void shouldLikeAndSaveOffline() {
        final PublishSubject<Void> offlineObservable = PublishSubject.create();
        when(offlineOperations.makePlaylistAvailableOffline(playlistItem.getUrn())).thenReturn(offlineObservable);
        when(likeOperations.toggleLike(playlistItem.getUrn(), !playlistItem.isUserLike())).thenReturn(Observable.just(LikeOperations.LikeResult.LIKE_SUCCEEDED));
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_available);

        presenter.show(button, playlistItem);
        presenter.saveOffline(playlistItem);

        assertThat(offlineObservable.hasObservers()).isTrue();
        verify(likeOperations).toggleLike(playlistItem.getUrn(), true);
    }

    @Test
    public void clickingOnMakeOfflineAvailablePublishTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_available);

        presenter.show(button, playlistItem);
        presenter.saveOffline(playlistItem);

        OfflineInteractionEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING,
                                                                     OfflineInteractionEvent.class);
        assertThat(trackingEvent.context().get()).isEqualTo(OfflineInteractionEvent.Context.PLAYLIST_CONTEXT);
        assertThat(trackingEvent.isEnabled().get()).isEqualTo(true);
        assertThat(trackingEvent.clickName().get()).isEqualTo(OfflineInteractionEvent.Kind.KIND_OFFLINE_PLAYLIST_ADD);
        assertThat(trackingEvent.clickObject().get()).isEqualTo(playlistItem.getUrn());
    }

    @Test
    public void clickingOnMakeOfflineUnavailableRemovedPlaylistFromOfflineContent() {
        final PublishSubject<Void> offlineObservable = PublishSubject.create();
        when(offlineOperations.makePlaylistUnavailableOffline(playlistItem.getUrn())).thenReturn(offlineObservable);
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_unavailable);

        presenter.show(button, playlistItem);
        presenter.removeFromOffline(context);

        assertThat(offlineObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickIgnoredWhileShowing() {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlist));

        presenter.show(button, playlistItem);
        presenter.show(button, playlistItem);

        verify(playlistMenuRenderer, times(1)).render(playlistItem);
    }

    @Test
    public void clickAfterDismissalShowsPopup() {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlist));

        presenter.show(button, playlistItem);
        presenter.onDismiss();
        presenter.show(button, playlistItem);

        verify(playlistMenuRenderer, times(2)).render(playlistItem);
    }

    @Test
    public void clickingOnMakeOfflineUnavailablePublishTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_unavailable);

        presenter.show(button, playlistItem);
        presenter.removeFromOffline(context);

        OfflineInteractionEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING,
                                                                     OfflineInteractionEvent.class);
        assertThat(trackingEvent.context().get()).isEqualTo(OfflineInteractionEvent.Context.PLAYLIST_CONTEXT);
        assertThat(trackingEvent.isEnabled().get()).isEqualTo(false);
        assertThat(trackingEvent.clickName().get()).isEqualTo(OfflineInteractionEvent.Kind.KIND_OFFLINE_PLAYLIST_REMOVE);
        assertThat(trackingEvent.clickObject().get()).isEqualTo(playlistItem.getUrn());
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/android-listeners/issues/3431")
    public void clickingOnUnlikeNotOwnedPlaylistRemovesItFromOfflineContent() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        ApiPlaylist playlist1 = ModelFixtures.create(ApiPlaylist.class);
        PlaylistItem likedPlaylist = PlayableFixtures.fromApiPlaylist(playlist1, true, false, false);

        playlistItem = likedPlaylist;
        presenter.show(button, playlistItem);

        presenter.handleLike(playlistItem);

        verify(offlineOperations).makePlaylistUnavailableOffline(playlistItem.getUrn());
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/android-listeners/issues/3431")
    public void clickingOnUnlikeOwnPlaylistDoesNotRemoveItFromOfflineContent() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        ApiPlaylist playlist1 = ModelFixtures.create(ApiPlaylist.class);
        PlaylistItem likedAndPostedPlaylist = PlayableFixtures.fromApiPlaylist(playlist1, true, false, false);
        playlistItem = likedAndPostedPlaylist;

        when(accountOperations.isLoggedInUser(playlistItem.creatorUrn())).thenReturn(true);


        presenter.show(button, playlistItem);

        presenter.handleLike(playlistItem);

        verify(offlineOperations, never()).makePlaylistUnavailableOffline(playlistItem.getUrn());
    }

    @Test
    public void shouldPlayNext() {
        when(menuItem.getItemId()).thenReturn(R.id.play_next);

        presenter.show(button, playlistItem);
        presenter.handlePlayNext();

        verify(playQueueHelper, times(1)).playNext(any(Urn.class));
    }

    @Test
    public void clickingOnPlayNextShouldPublishTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.play_next);

        presenter.show(button, playlistItem);
        presenter.handlePlayNext();

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.PLAY_NEXT);
        assertThat(event.clickObjectUrn().get()).isEqualTo(playlistItem.getUrn());
        assertThat(event.originScreen().get()).isEqualTo(SCREEN);
    }

}
