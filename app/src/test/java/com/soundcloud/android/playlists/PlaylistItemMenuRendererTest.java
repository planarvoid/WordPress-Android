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
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
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
    @Mock private FeatureFlags featureFlags;
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
                                                featureFlags);

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
        when(featureFlags.isEnabled(Flag.PLAY_QUEUE)).thenReturn(false);

        renderer.render(playlist);

        verify(popupMenuWrapper).setItemVisible(R.id.play_next, false);
    }

    @Test
    public void showPlaylist() {
        when(featureFlags.isEnabled(Flag.PLAY_QUEUE)).thenReturn(true);

        renderer.render(playlist);

        verify(popupMenuWrapper).setItemVisible(R.id.play_next, true);
    }

}
