package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PlaylistDetailsControllerTest extends AndroidUnitTest {

    @Mock private PlaylistTrackItemRenderer trackItemRenderer;
    @Mock private PlaylistUpsellItemRenderer upsellItemRenderer;
    @Mock private PlaylistUpsellOperations upsellOperations;
    @Mock private ListItemAdapter<TypedListItem> itemAdapter;
    @Mock private Navigator navigator;

    private TestEventBus eventBus = new TestEventBus();
    private PlaylistWithTracks playlist;
    private PlaylistDetailsController controller;
    private List<TypedListItem> adapterItems;

    @Before
    public void setUp() throws Exception {
        controller = new PlaylistDetailsControllerImpl(trackItemRenderer, upsellItemRenderer, upsellOperations, itemAdapter, eventBus, navigator);
        playlist = createPlaylist();
        adapterItems = new ArrayList<>(listItems().size());
        adapterItems.addAll(listItems());
    }

    @Test
    public void hasTracksIsFalseIfAdapterDataIsEmpty() throws Exception {
        assertThat(controller.hasTracks()).isFalse();
    }

    @Test
    public void hasTracksIsTrueIfAdapterDataIsNotEmpty() throws Exception {
        when(itemAdapter.getItems()).thenReturn(adapterItems);
        assertThat(controller.hasTracks()).isTrue();
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterWhenPlaylistIsReturned() throws Exception {
        when(upsellOperations.toListItems(playlist)).thenReturn(listItems());
        controller.setContent(playlist, null);

        InOrder inOrder = Mockito.inOrder(itemAdapter);
        inOrder.verify(itemAdapter).clear();
        for (TypedListItem item : listItems()) {
            inOrder.verify(itemAdapter).addItem(item);
        }
    }

    @Test
    public void onUpsellItemDismissedUpsellsGetDisabled() {
        controller.onUpsellItemDismissed(0);

        verify(upsellOperations).disableUpsell();
    }

    @Test
    public void onUpsellItemClickedOpensUpgradeScreen() {
        controller.onUpsellItemClicked(context());

        verify(navigator).openUpgrade(context());
    }

    @Test
    public void onUpsellItemCreatedSendsUpsellTrackingEvent() {
        UpgradeFunnelEvent expectedEvent = UpgradeFunnelEvent.forPlaylistTracksImpression();

        controller.onUpsellItemCreated();

        UpgradeFunnelEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(trackingEvent.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    @Test
    public void onUpsellItemClickedSendsUpsellTrackingEvent() {
        UpgradeFunnelEvent expectedEvent = UpgradeFunnelEvent.forPlaylistTracksClick();

        controller.onUpsellItemClicked(context());

        UpgradeFunnelEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(trackingEvent.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    private PlaylistWithTracks createPlaylist() {
        return new PlaylistWithTracks(
                ModelFixtures.create(ApiPlaylist.class).toPropertySet(),
                ModelFixtures.trackItems(10));
    }

    private static class PlaylistDetailsControllerImpl extends PlaylistDetailsController {

        protected PlaylistDetailsControllerImpl(PlaylistTrackItemRenderer trackPresenter,
                                                PlaylistUpsellItemRenderer upsellItemRenderer,
                                                PlaylistUpsellOperations playlistUpsellOperations,
                                                ListItemAdapter<TypedListItem> adapter,
                                                EventBus eventBus,
                                                Navigator navigator) {
            super(trackPresenter, upsellItemRenderer, adapter, playlistUpsellOperations, eventBus, navigator);
        }

        @Override
        void setEmptyStateMessage(String title, String description) {
            //no op
        }

        @Override
        boolean hasContent() {
            return false;
        }

        @Override
        void setListShown(boolean show) {

        }

        @Override
        public void setEmptyViewStatus(EmptyView.Status status) {

        }
    }

    private List<TypedListItem> listItems() {
        final LinkedList<TypedListItem> items = new LinkedList<>();
        items.addAll(playlist.getTracks());
        return items;
    }

}
