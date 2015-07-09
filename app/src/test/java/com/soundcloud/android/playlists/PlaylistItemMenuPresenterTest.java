package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.annotations.Issue;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

public class PlaylistItemMenuPresenterTest extends AndroidUnitTest {

    @Mock private Context context;
    @Mock private PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock private PopupMenuWrapper popupMenuWrapper;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private LikeOperations likeOperations;
    @Mock private ScreenProvider screenProvider;
    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private Navigator navigator;
    @Mock private MenuItem menuItem;
    @Mock private View button;

    private final TestEventBus eventBus = new TestEventBus();

    private PlaylistItemMenuPresenter presenter;
    private  PlaylistItem playlist = createPlaylistItem();

    @Before
    public void setUp() throws Exception {
        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(popupMenuWrapper.findItem(anyInt())).thenReturn(menuItem);
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.<PlaylistWithTracks>empty());

        when(offlineContentOperations.makePlaylistAvailableOffline(any(Urn.class))).thenReturn(Observable.<Boolean>empty());
        when(offlineContentOperations.makePlaylistUnavailableOffline(any(Urn.class))).thenReturn(Observable.<Boolean>empty());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        when(likeOperations.toggleLike(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.<PropertySet>empty());

        when(screenProvider.getLastScreenTag()).thenReturn("some tag");
        presenter = new PlaylistItemMenuPresenter(context, eventBus, popupMenuWrapperFactory, playlistOperations,
                likeOperations, screenProvider, featureOperations, offlineContentOperations, navigator);

        presenter.show(button, playlist, true);
    }

    @Test
    public void clickingOnUpsellItemNavigatesToUpgrade() {
        when(menuItem.getItemId()).thenReturn(R.id.upsell_offline_content);

        presenter.onMenuItemClick(menuItem, context);

        verify(navigator).openUpgrade(context);
    }

    @Test
    public void clickingOnAddToLikesAddPlaylistLike() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.onMenuItemClick(menuItem, context);

        verify(likeOperations).toggleLike(playlist.getEntityUrn(), !playlist.isLiked());
    }

    @Test
    public void clickingOnAddToLikesSendsTrackingEvents() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.onMenuItemClick(menuItem, context);

        TrackingEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(trackingEvent.getAttributes()
                .containsValue(String.valueOf(playlist.getEntityUrn().getNumericId()))).isTrue();
    }

    @Test
    public void clickingOnMakeOfflineAvailableMarksPlaylistAsOfflineContent() {
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_available);

        presenter.onMenuItemClick(menuItem, context);

        verify(offlineContentOperations).makePlaylistAvailableOffline(playlist.getEntityUrn());
    }

    @Test
    public void clickingOnMakeOfflineUnavailableRemovedPlaylistFromOfflineContent() {
        when(menuItem.getItemId()).thenReturn(R.id.make_offline_unavailable);

        presenter.onMenuItemClick(menuItem, context);

        verify(offlineContentOperations).makePlaylistUnavailableOffline(playlist.getEntityUrn());
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/SoundCloud-Android/issues/3431")
    public void clickingOnUnlikeNotOwnedPlaylistRemovesItFromOfflineContent() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        ApiPlaylist playlist1 = ModelFixtures.create(ApiPlaylist.class);
        PropertySet likedPlaylist = TestPropertySets.fromApiPlaylist(playlist1, true, false, false, false);

        playlist = PlaylistItem.from(likedPlaylist);
        presenter.show(button, playlist, true);

        presenter.onMenuItemClick(menuItem, context);

        verify(offlineContentOperations).makePlaylistUnavailableOffline(playlist.getEntityUrn());
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/SoundCloud-Android/issues/3431")
    public void clickingOnUnlikeOwnPlaylistDoesNotRemoveItFromOfflineContent() {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        ApiPlaylist playlist1 = ModelFixtures.create(ApiPlaylist.class);
        PropertySet likedAndPostedPlaylist = TestPropertySets.fromApiPlaylist(playlist1, true, false, false, true);
        playlist = PlaylistItem.from(likedAndPostedPlaylist);
        presenter.show(button, playlist, true);

        presenter.onMenuItemClick(menuItem, context);

        verify(offlineContentOperations, never()).makePlaylistUnavailableOffline(playlist.getEntityUrn());
    }

    private PlaylistItem createPlaylistItem() {
        return PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
    }

}