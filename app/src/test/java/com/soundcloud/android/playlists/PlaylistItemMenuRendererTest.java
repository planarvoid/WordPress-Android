package com.soundcloud.android.playlists;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
    @Mock private ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    private PlaylistItem playlist = ModelFixtures.playlistItem();
    private PlaylistItemMenuRenderer renderer;

    @Before
    public void setUp() throws Exception {
        when(button.getContext()).thenReturn(context());
        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(popupMenuWrapper.findItem(anyInt())).thenReturn(menuItem);
        renderer = new PlaylistItemMenuRenderer(listener,
                                                button,
                                                popupMenuWrapperFactory,
                                                accountOperations,
                                                screenProvider,
                                                eventBus,
                                                featureOperations,
                                                changeLikeToSaveExperimentStringHelper);
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
    public void shouldGetLikeActionTitle() {
        PlaylistItem playlistItem = buildPlaylistWithUserLike(false);

        renderer.render(playlistItem);

        verify(changeLikeToSaveExperimentStringHelper).getString(ExperimentString.LIKE);
        verify(popupMenuWrapper).setItemVisible(R.id.add_to_likes, true);
    }

    @Test
    public void shouldGetUnlikeActionTitle() {
        PlaylistItem playlistItem = buildPlaylistWithUserLike(true);

        renderer.render(playlistItem);

        verify(changeLikeToSaveExperimentStringHelper).getString(ExperimentString.UNLIKE);
        verify(popupMenuWrapper).setItemVisible(R.id.add_to_likes, true);
    }

    @Test
    public void showOfflineDownloadOptionWhenNotMarkedForOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        PlaylistItem playlistItem = buildPlaylistWithMarkedForOffline(false);

        renderer.render(playlistItem);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, true);
        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_unavailable, false);
        verify(popupMenuWrapper).setItemVisible(R.id.upsell_offline_content, false);
    }

    @Test
    public void showOfflineRemovalOptionWhenMarkedForOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        PlaylistItem playlistItem = buildPlaylistWithMarkedForOffline(true);

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
        PlaylistItem playlistItem = buildPlaylistWithMarkedForOffline(false);

        renderer.render(playlistItem);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, false);
        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_unavailable, false);
        verify(popupMenuWrapper).setItemVisible(R.id.upsell_offline_content, true);
    }

    @Test
    public void hideAllOfflineContentOptionsWhenOfflineContentAndUpsellContentAreDisabled() throws Exception {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);
        PlaylistItem playlistItem = buildPlaylistWithMarkedForOffline(true);

        renderer.render(playlistItem);

        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_available, false);
        verify(popupMenuWrapper).setItemVisible(R.id.make_offline_unavailable, false);
        verify(popupMenuWrapper).setItemVisible(R.id.upsell_offline_content, false);
    }

    private PlaylistItem buildPlaylistWithMarkedForOffline(boolean markedForOffline) {
        return ModelFixtures.playlistItemBuilder().isMarkedForOffline(Optional.of(markedForOffline)).build();
    }

    private PlaylistItem buildPlaylistWithUserLike(boolean userLike) {
        return ModelFixtures.playlistItemBuilder().isUserLike(userLike).build();
    }

}
