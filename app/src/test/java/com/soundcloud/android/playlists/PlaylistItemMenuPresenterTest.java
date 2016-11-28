package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.soundcloud.android.events.PlayableTrackingKeys;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.annotations.Issue;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.OverflowMenuOptions;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
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
    @Mock private ShareOperations shareOperations;
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
    private PlaylistItem playlist = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
    private OverflowMenuOptions menuOptions = OverflowMenuOptions.builder().showOffline(true).build();

    @Before
    public void setUp() throws Exception {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.<PlaylistWithTracks>empty());
        when(playlistOperations.trackUrnsForPlayback(any(Urn.class)))
                .thenReturn(Observable.<List<Urn>>just(Lists.newArrayList(Urn.NOT_SET)));


        when(offlineOperations.makePlaylistAvailableOffline(any(Urn.class))).thenReturn(Observable.<Void>empty());
        when(offlineOperations.makePlaylistUnavailableOffline(any(Urn.class))).thenReturn(Observable.<Void>empty());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        when(likeOperations.toggleLike(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.<PropertySet>empty());

        when(screenProvider.getLastScreenTag()).thenReturn(SCREEN);
        presenter = new PlaylistItemMenuPresenter(context,
                                                  eventBus,
                                                  playlistOperations,
                                                  likeOperations,
                                                  repostOperations,
                                                  shareOperations,
                                                  screenProvider,
                                                  featureOperations,
                                                  offlineOperations,
                                                  navigator,
                                                  playQueueHelper,
                                                  eventTracker,
                                                  playlistMenuRenderFactory);

        button = new View(activity());

        when(playlistMenuRenderFactory.create(presenter, button, menuOptions)).thenReturn(playlistMenuRenderer);
    }

    @Test
    public void clickingOnUpsellItemNavigatesToUpgrade() {
        when(menuItem.getItemId()).thenReturn(R.id.upsell_offline_content);

        presenter.show(button, playlist, menuOptions);
        presenter.handleUpsell(context);

        verify(navigator).openUpgrade(context);
    }

    @Test
    public void clickingOnAddToLikesAddPlaylistLike() {
        final PublishSubject<PropertySet> likeObservable = PublishSubject.create();
        when(likeOperations.toggleLike(playlist.getUrn(), !playlist.isLiked())).thenReturn(likeObservable);
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.show(button, playlist, menuOptions);
        presenter.handleLike(playlist);

        assertThat(likeObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickRepostItemRepostsPlaylist() {
        final PublishSubject<PropertySet> repostObservable = PublishSubject.create();
        when(repostOperations.toggleRepost(playlist.getUrn(), !playlist.isRepostedByCurrentUser())).thenReturn(
                repostObservable);
        when(menuItem.getItemId()).thenReturn(R.id.toggle_repost);

        presenter.show(button, playlist, menuOptions);
        presenter.handleRepost(!playlist.isRepostedByCurrentUser());

        assertThat(repostObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickingOnRepostSendsTrackingEvent() {
        final PublishSubject<PropertySet> repostObservable = PublishSubject.create();
        when(repostOperations.toggleRepost(playlist.getUrn(), !playlist.isRepostedByCurrentUser())).thenReturn(
                repostObservable);
        when(menuItem.getItemId()).thenReturn(R.id.toggle_repost);

        presenter.show(button, playlist, menuOptions);
        presenter.handleRepost(!playlist.isRepostedByCurrentUser());

        verify(eventTracker).trackEngagement(uiEventArgumentCaptor.capture());

        UIEvent uiEvent = uiEventArgumentCaptor.getValue();
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.isFromOverflow()).isTrue();
    }

    @Test
    public void clickingOnShareItemSharesPlaylist() {
        when(menuItem.getItemId()).thenReturn(R.id.share);

        presenter.show(button, playlist, menuOptions);
        presenter.handleShare(context, playlist);

        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                        .contextScreen(screenProvider.getLastScreenTag())
                                                                        .pageName(screenProvider.getLastScreenTag())
                                                                        .isFromOverflow(true)
                                                                        .invokerScreen(ScreenElement.LIST.get())
                                                                        .build();

        verify(shareOperations).share(context,
                                      playlist.getSource().get(PlayableProperty.PERMALINK_URL),
                                      eventContextMetadata, null,
                                      EntityMetadata.from(playlist.getSource()));
    }

    @Test
    public void clickingOnAddToLikesSendsTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.show(button, playlist, menuOptions);
        presenter.handleLike(playlist);

        verify(eventTracker).trackEngagement(uiEventArgumentCaptor.capture());

        UIEvent uiEvent = uiEventArgumentCaptor.getValue();
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.isFromOverflow()).isTrue();
    }

    @Test
    public void clickingOnMakeOfflineAvailableMarksPlaylistAsOfflineContent() {
        final PublishSubject<Void> offlineObservable = PublishSubject.create();
        when(offlineOperations.makePlaylistAvailableOffline(playlist.getUrn())).thenReturn(offlineObservable);
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_available);

        presenter.show(button, playlist, menuOptions);
        presenter.saveOffline();

        assertThat(offlineObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickingOnMakeOfflineAvailablePublishTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_available);

        presenter.show(button, playlist, menuOptions);
        presenter.saveOffline();

        OfflineInteractionEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING,
                                                                     OfflineInteractionEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(OfflineInteractionEvent.KIND_OFFLINE_PLAYLIST_ADD);
        assertThat(trackingEvent.getAttributes()
                                .containsValue(String.valueOf(playlist.getUrn()))).isTrue();
    }

    @Test
    public void clickingOnMakeOfflineUnavailableRemovedPlaylistFromOfflineContent() {
        final PublishSubject<Void> offlineObservable = PublishSubject.create();
        when(offlineOperations.makePlaylistUnavailableOffline(playlist.getUrn())).thenReturn(offlineObservable);
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_unavailable);

        presenter.show(button, playlist, menuOptions);
        presenter.removeFromOffline(context);

        assertThat(offlineObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickingOnMakeOfflineUnavailablePublishTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_unavailable);

        presenter.show(button, playlist, menuOptions);
        presenter.removeFromOffline(context);

        OfflineInteractionEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING,
                                                                     OfflineInteractionEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(OfflineInteractionEvent.KIND_OFFLINE_PLAYLIST_REMOVE);
        assertThat(trackingEvent.getAttributes()
                                .containsValue(String.valueOf(playlist.getUrn()))).isTrue();
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/SoundCloud-Android/issues/3431")
    public void clickingOnUnlikeNotOwnedPlaylistRemovesItFromOfflineContent() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        ApiPlaylist playlist1 = ModelFixtures.create(ApiPlaylist.class);
        PropertySet likedPlaylist = TestPropertySets.fromApiPlaylist(playlist1, true, false, false, false);

        playlist = PlaylistItem.from(likedPlaylist);
        presenter.show(button, playlist, menuOptions);

        presenter.handleLike(playlist);

        verify(offlineOperations).makePlaylistUnavailableOffline(playlist.getUrn());
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/SoundCloud-Android/issues/3431")
    public void clickingOnUnlikeOwnPlaylistDoesNotRemoveItFromOfflineContent() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        ApiPlaylist playlist1 = ModelFixtures.create(ApiPlaylist.class);
        PropertySet likedAndPostedPlaylist = TestPropertySets.fromApiPlaylist(playlist1, true, false, false, true);
        playlist = PlaylistItem.from(likedAndPostedPlaylist);
        presenter.show(button, playlist, menuOptions);

        presenter.handleLike(playlist);

        verify(offlineOperations, never()).makePlaylistUnavailableOffline(playlist.getUrn());
    }

    @Test
    public void shouldPlayNext() {
        when(menuItem.getItemId()).thenReturn(R.id.play_next);

        presenter.show(button, playlist, menuOptions);
        presenter.handlePlayNext();

        verify(playQueueHelper, times(1)).playNext(any(Urn.class));
    }

    @Test
    public void clickingOnPlayNextShouldPublishTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.play_next);

        presenter.show(button, playlist, menuOptions);
        presenter.handlePlayNext();

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_PLAY_NEXT);
        assertThat(event.get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN)).isEqualTo(playlist.getUrn().toString());
        assertThat(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN)).isEqualTo(SCREEN);
    }

}
