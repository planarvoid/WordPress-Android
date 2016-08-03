package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.annotations.Issue;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.OverflowMenuOptions;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

public class PlaylistItemMenuPresenterTest extends AndroidUnitTest {

    @Mock private Context context;
    @Mock private FragmentActivity activity;
    @Mock private PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock private PopupMenuWrapper popupMenuWrapper;
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

    private View button;

    private final TestEventBus eventBus = new TestEventBus();

    private PlaylistItemMenuPresenter presenter;
    private PlaylistItem playlist = createPlaylistItem();
    private OverflowMenuOptions menuOptions = OverflowMenuOptions.builder().showOffline(true).build();

    @Before
    public void setUp() throws Exception {
        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(popupMenuWrapper.findItem(anyInt())).thenReturn(menuItem);
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.<PlaylistWithTracks>empty());
        when(playlistOperations.trackUrnsForPlayback(any(Urn.class)))
                .thenReturn(Observable.<List<Urn>>just(Lists.newArrayList(Urn.NOT_SET)));


        when(offlineOperations.makePlaylistAvailableOffline(any(Urn.class))).thenReturn(Observable.<Void>empty());
        when(offlineOperations.makePlaylistUnavailableOffline(any(Urn.class))).thenReturn(Observable.<Void>empty());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        when(likeOperations.toggleLike(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.<PropertySet>empty());

        when(screenProvider.getLastScreenTag()).thenReturn("some tag");
        presenter = new PlaylistItemMenuPresenter(context,
                                                  eventBus,
                                                  popupMenuWrapperFactory,
                                                  accountOperations,
                                                  playlistOperations,
                                                  likeOperations,
                                                  repostOperations,
                                                  shareOperations,
                                                  screenProvider,
                                                  featureOperations,
                                                  offlineOperations,
                                                  navigator,
                                                  featureFlags,
                                                  playQueueHelper);

        button = new View(new FragmentActivity());
    }

    @Test
    public void clickingOnUpsellItemNavigatesToUpgrade() {
        when(menuItem.getItemId()).thenReturn(R.id.upsell_offline_content);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, context);

        verify(navigator).openUpgrade(context);
    }

    @Test
    public void clickingOnAddToLikesAddPlaylistLike() {
        final PublishSubject<PropertySet> likeObservable = PublishSubject.create();
        when(likeOperations.toggleLike(playlist.getUrn(), !playlist.isLiked())).thenReturn(likeObservable);
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, context);

        assertThat(likeObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickRepostItemRepostsPlaylist() {
        final PublishSubject<PropertySet> repostObservable = PublishSubject.create();
        when(repostOperations.toggleRepost(playlist.getUrn(), !playlist.isReposted())).thenReturn(repostObservable);
        when(menuItem.getItemId()).thenReturn(R.id.toggle_repost);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, context);

        assertThat(repostObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickingOnRepostSendsTrackingEvent() {
        final PublishSubject<PropertySet> repostObservable = PublishSubject.create();
        when(repostOperations.toggleRepost(playlist.getUrn(), !playlist.isReposted())).thenReturn(repostObservable);
        when(menuItem.getItemId()).thenReturn(R.id.toggle_repost);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, context);

        UIEvent uiEvent = eventBus.lastEventOn(EventQueue.TRACKING, UIEvent.class);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.isFromOverflow()).isTrue();
    }

    @Test
    public void clickingOnShareItemSharesPlaylist() {
        when(menuItem.getItemId()).thenReturn(R.id.share);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, context);

        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                        .contextScreen(screenProvider.getLastScreenTag())
                                                                        .pageName(screenProvider.getLastScreenTag())
                                                                        .isFromOverflow(true)
                                                                        .invokerScreen(ScreenElement.LIST.get())
                                                                        .build();

        verify(shareOperations).share(context, playlist.getSource(), eventContextMetadata, null);
    }

    @Test
    public void clickingOnAddToLikesSendsTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, context);

        UIEvent uiEvent = eventBus.lastEventOn(EventQueue.TRACKING, UIEvent.class);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.isFromOverflow()).isTrue();
    }

    @Test
    public void clickingOnMakeOfflineAvailableMarksPlaylistAsOfflineContent() {
        final PublishSubject<Void> offlineObservable = PublishSubject.create();
        when(offlineOperations.makePlaylistAvailableOffline(playlist.getUrn())).thenReturn(offlineObservable);
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_available);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, context);

        assertThat(offlineObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickingOnMakeOfflineAvailablePublishTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_available);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, context);

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
        presenter.onMenuItemClick(menuItem, activity);

        assertThat(offlineObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickingOnMakeOfflineUnavailablePublishTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_unavailable);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, activity);

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

        presenter.onMenuItemClick(menuItem, context);

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

        presenter.onMenuItemClick(menuItem, context);

        verify(offlineOperations, never()).makePlaylistUnavailableOffline(playlist.getUrn());
    }

    @Test
    public void showDeletePlaylistWhenOwnedByCurrentUser() {
        when(accountOperations.isLoggedInUser(playlist.getCreatorUrn())).thenReturn(true);

        presenter.show(button, playlist, menuOptions);

        verify(popupMenuWrapper).setItemVisible(R.id.delete_playlist, true);
    }

    @Test
    public void doNotShowDeletePlaylistWhenNotOwnedByCurrentUser() {
        when(accountOperations.isLoggedInUser(playlist.getCreatorUrn())).thenReturn(false);

        presenter.show(button, playlist, menuOptions);

        verify(popupMenuWrapper).setItemVisible(R.id.delete_playlist, false);
    }

    @Test
    public void doNotShowPlaylist() {
        when(featureFlags.isEnabled(Flag.PLAY_QUEUE)).thenReturn(false);

        presenter.show(button, playlist, menuOptions);

        verify(popupMenuWrapper).setItemVisible(R.id.play_next, false);
    }

    @Test
    public void showPlaylist() {
        when(featureFlags.isEnabled(Flag.PLAY_QUEUE)).thenReturn(true);

        presenter.show(button, playlist, menuOptions);

        verify(popupMenuWrapper).setItemVisible(R.id.play_next, true);
    }

    @Test
    public void shouldPlayNext() {
        when(menuItem.getItemId()).thenReturn(R.id.play_next);

        presenter.show(button, playlist, menuOptions);
        presenter.onMenuItemClick(menuItem, context);

        verify(playQueueHelper, times(1)).playNext(any(Urn.class));
    }

    private PlaylistItem createPlaylistItem() {
        return PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
    }

}
