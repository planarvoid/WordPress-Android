package com.soundcloud.android.playlists;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.PlayQueueConfiguration;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.OverflowMenuOptions;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

public class PlaylistItemMenuRendererTest extends AndroidUnitTest {

    @Mock private PlaylistItemMenuRenderer.Listener listener;
    @Mock private ScreenProvider screenProvider;
    @Mock private EventBus eventBus;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlayQueueConfiguration playQueueConfiguration;
    @Mock private PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock private PopupMenuWrapper popupMenuWrapper;
    @Mock private AccountOperations accountOperations;
    @Mock private View button;
    @Mock private MenuItem menuItem;

    private PlaylistItem playlist = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
    private OverflowMenuOptions menuOptions = OverflowMenuOptions.builder().showOffline(true).build();
    private PlaylistItemMenuRenderer renderer;

    @Before
    public void setUp() throws Exception {
        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(popupMenuWrapper.findItem(anyInt())).thenReturn(menuItem);
        renderer = new PlaylistItemMenuRenderer(listener,
                                                button,
                                                menuOptions,
                                                popupMenuWrapperFactory,
                                                accountOperations,
                                                screenProvider,
                                                eventBus,
                                                featureOperations,
                                                playQueueConfiguration);
    }

    @Test
    public void showDeletePlaylistWhenOwnedByCurrentUser() {
        when(accountOperations.isLoggedInUser(playlist.getCreatorUrn())).thenReturn(true);

        renderer.render(playlist);

        verify(popupMenuWrapper).setItemVisible(R.id.delete_playlist, true);
    }

    @Test
    public void doNotShowDeletePlaylistWhenNotOwnedByCurrentUser() {
        when(accountOperations.isLoggedInUser(playlist.getCreatorUrn())).thenReturn(false);

        renderer.render(playlist);

        verify(popupMenuWrapper).setItemVisible(R.id.delete_playlist, false);
    }

    @Test
    public void doNotShowPlaylist() {
        when(playQueueConfiguration.isEnabled()).thenReturn(false);

        renderer.render(playlist);

        verify(popupMenuWrapper).setItemVisible(R.id.play_next, false);
    }

    @Test
    public void showPlaylist() {
        when(playQueueConfiguration.isEnabled()).thenReturn(true);

        renderer.render(playlist);

        verify(popupMenuWrapper).setItemVisible(R.id.play_next, true);
    }

    @Test
    public void doNotShowOfflineWhenNotMarkedForDownloadAndNotLikedAndNotPosted() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        PlaylistItem nonDownloadablePlaylist = buildPlaylist(false, false);
        when(accountOperations.isLoggedInUser(nonDownloadablePlaylist.getCreatorUrn())).thenReturn(false);

        renderer.render(nonDownloadablePlaylist);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, false);
        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_unavailable, false);
        verify(popupMenuWrapper).setItemVisible(R.id.upsell_offline_content, false);
    }

    @Test
    public void showOfflineWhenMarkedForDownloadAndNotLikedAndNotPosted() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        PlaylistItem nonDownloadablePlaylist = buildPlaylist(true, false);
        when(accountOperations.isLoggedInUser(nonDownloadablePlaylist.getCreatorUrn())).thenReturn(false);

        renderer.render(nonDownloadablePlaylist);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_unavailable, true);
    }

    @Test
    public void showOfflineWhenLikedAndNotMarkedForDownloadAndNotPosted() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        PlaylistItem likedPlaylist = buildPlaylist(false, true);
        when(accountOperations.isLoggedInUser(likedPlaylist.getCreatorUrn())).thenReturn(false);

        renderer.render(likedPlaylist);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, true);
    }

    @Test
    public void showOfflineWhenPostedAndNotMarkedForDownloadAndLiked() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        PlaylistItem postedPlaylist = buildPlaylist(false, false);
        when(accountOperations.isLoggedInUser(postedPlaylist.getCreatorUrn())).thenReturn(true);

        renderer.render(postedPlaylist);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, true);
    }

    private PlaylistItem buildPlaylist(boolean markedForDownload, boolean liked) {
        PlaylistItem playlist = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
        playlist.getSource().put(OfflineProperty.IS_MARKED_FOR_OFFLINE, markedForDownload);
        playlist.getSource().put(PlaylistProperty.IS_USER_LIKE, liked);
        return playlist;
    }

}
