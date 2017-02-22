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
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.OverflowMenuOptions;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.optional.Optional;
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
        when(button.getContext()).thenReturn(context());
        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(popupMenuWrapper.findItem(anyInt())).thenReturn(menuItem);
        renderer = new PlaylistItemMenuRenderer(listener,
                                                button,
                                                menuOptions,
                                                popupMenuWrapperFactory,
                                                accountOperations,
                                                screenProvider,
                                                eventBus,
                                                featureOperations);
    }

    @Test
    public void showDeletePlaylistWhenOwnedByCurrentUser() {
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);

        renderer.render(playlist);

        verify(popupMenuWrapper).setItemVisible(R.id.delete_playlist, true);
    }

    @Test
    public void doNotShowDeletePlaylistWhenNotOwnedByCurrentUser() {
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(false);

        renderer.render(playlist);

        verify(popupMenuWrapper).setItemVisible(R.id.delete_playlist, false);
    }

    @Test
    public void showPlayNext() {
        renderer.render(playlist);

        verify(popupMenuWrapper).setItemVisible(R.id.play_next, true);
    }

    @Test
    public void showOfflineDownloadOptionWhenNotMarkedForOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        PlaylistItem playlistItem = buildPlaylist(false);

        renderer.render(playlistItem);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, true);
        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_unavailable, false);
        verify(popupMenuWrapper).setItemVisible(R.id.upsell_offline_content, false);
    }

    @Test
    public void showOfflineRemovalOptionWhenMarkedForOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        PlaylistItem playlistItem = buildPlaylist(true);

        renderer.render(playlistItem);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, false);
        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_unavailable, true);
        verify(popupMenuWrapper).setItemVisible(R.id.upsell_offline_content, false);
    }

    @Test
    public void showUpsellOption() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(true);
        when(menuItem.isVisible()).thenReturn(true);
        when(screenProvider.getLastScreenTag()).thenReturn("screen-tag");
        PlaylistItem playlistItem = buildPlaylist(false);

        renderer.render(playlistItem);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, false);
        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_unavailable, false);
        verify(popupMenuWrapper).setItemVisible(R.id.upsell_offline_content, true);
    }

    @Test
    public void hideAllOfflineContentOptionsWhenOfflineContentAndUpsellContentAreDisabled() throws Exception {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);
        PlaylistItem playlistItem = buildPlaylist(true);

        renderer.render(playlistItem);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, false);
        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_unavailable, false);
        verify(popupMenuWrapper).setItemVisible(R.id.upsell_offline_content, false);
    }

    private PlaylistItem buildPlaylist(boolean markedForDownload) {
        return ModelFixtures.playlistItemBuilder().isMarkedForOffline(Optional.of(markedForDownload)).build();
    }

}
