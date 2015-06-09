package com.soundcloud.android.playlists;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistItemMenuPresenterTest {

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

    private EventBus eventBus = new TestEventBus();

    private PlaylistItemMenuPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(playlistOperations.playlist(Urn.forPlaylist(123L))).thenReturn(Observable.<PlaylistWithTracks>empty());
        presenter = new PlaylistItemMenuPresenter(context, eventBus, popupMenuWrapperFactory, playlistOperations,
                likeOperations, screenProvider, featureOperations, offlineContentOperations, navigator);
    }

    @Test
    public void clickingOnUpsellItemNavigatesToUpgrade() {
        when(menuItem.getItemId()).thenReturn(R.id.upsell_offline_content);

        presenter.onMenuItemClick(menuItem, context);

        verify(navigator).openUpgrade(context);
    }

}