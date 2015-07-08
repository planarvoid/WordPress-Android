package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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
import com.soundcloud.android.testsupport.PlatformUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

public class PlaylistItemMenuPresenterTest extends PlatformUnitTest {

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

    private final PlaylistItem playlist = createPlaylistItem();
    private final TestEventBus eventBus = new TestEventBus();

    private PlaylistItemMenuPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(popupMenuWrapper.findItem(anyInt())).thenReturn(menuItem);
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.<PlaylistWithTracks>empty());
        when(screenProvider.getLastScreenTag()).thenReturn("some tag");
        presenter = new PlaylistItemMenuPresenter(context, eventBus, popupMenuWrapperFactory, playlistOperations,
                likeOperations, screenProvider, featureOperations, offlineContentOperations, navigator);
    }

    @Test
    public void clickingOnUpsellItemNavigatesToUpgrade() {
        when(menuItem.getItemId()).thenReturn(R.id.upsell_offline_content);

        presenter.onMenuItemClick(menuItem, context);

        verify(navigator).openUpgrade(context);
    }

    @Test
    public void clickingOnAddToLikesAddPlaylistLike() {
        prepareForLikes(false);

        presenter.onMenuItemClick(menuItem, context);

        verify(likeOperations).toggleLike(playlist.getEntityUrn(), !playlist.isLiked());
    }

    @Test
    public void clickingOnAddToLikesSendsTrackingEvents() {
        prepareForLikes(false);

        presenter.onMenuItemClick(menuItem, context);

        TrackingEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(trackingEvent.getAttributes()
                .containsValue(String.valueOf(playlist.getEntityUrn().getNumericId()))).isTrue();
    }

    private PlaylistItem createPlaylistItem() {
        return PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
    }

    private void prepareForLikes(boolean isOfflineEnabled) {
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);
        when(likeOperations.toggleLike(playlist.getEntityUrn(), !playlist.isLiked()))
                .thenReturn(Observable.<PropertySet>empty());
        presenter.show(button, playlist, isOfflineEnabled);
    }

}