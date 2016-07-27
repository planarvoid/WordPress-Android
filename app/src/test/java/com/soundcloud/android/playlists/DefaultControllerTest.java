package com.soundcloud.android.playlists;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Resources;
import android.view.View;
import android.widget.ListView;

public class DefaultControllerTest extends AndroidUnitTest {

    private DefaultController controller;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaylistTrackItemRenderer trackRenderer;
    @Mock private PlaylistUpsellItemRenderer upsellItemRenderer;
    @Mock private PlaylistUpsellOperations playlistUpsellOperations;
    @Mock private InlinePlaylistTracksAdapter adapter;
    @Mock private ListView listView;
    @Mock private Resources resources;
    @Mock private View layout;
    @Mock private Navigator navigator;

    @Before
    public void setUp() throws Exception {
        when(layout.findViewById(android.R.id.list)).thenReturn(listView);
        when(adapter.getPlaylistItemRenderer()).thenReturn(trackRenderer);
        when(adapter.getUpsellItemRenderer()).thenReturn(upsellItemRenderer);
        controller = new DefaultController(adapter, playlistUpsellOperations, eventBus, navigator);
        controller.onViewCreated(layout, null);
    }

    @Test
    public void shouldListenForTrackPositionChangeEventsAndUpdateTrackPresenter() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(Urn.forTrack(123L)),
                                                                       Urn.NOT_SET,
                                                                       0));

        verify(trackRenderer).setPlayingTrack(Urn.forTrack(123L));
    }

    @Test
    public void shouldListenForNewPlayQueueEventsAndUpdateTrackPresenter() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(Urn.forTrack(123L)),
                                                                Urn.NOT_SET,
                                                                0));

        verify(trackRenderer).setPlayingTrack(Urn.forTrack(123L));
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesInOnDestroyView() {
        controller.onDestroyView();

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void setListShownWithTrueSetsListVisibilityToVisible() throws Exception {
        controller.setListShown(true);
        verify(listView).setVisibility(View.VISIBLE);
    }

    @Test
    public void setListShownWithFalseSetsListVisibilityToGone() throws Exception {
        controller.setListShown(false);
        verify(listView).setVisibility(View.GONE);
    }

    @Test
    public void setEmptyViewStatusSetsStateOnPresenterAndUpdatesAdapter() throws Exception {
        controller.setEmptyViewStatus(EmptyView.Status.OK);
        verify(adapter).setEmptyViewStatus(EmptyView.Status.OK);
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void hasContentShouldCheckInternalAdapterItemCount() {
        controller.hasContent();
        verify(adapter).hasContentItems();
    }

    @Test
    public void shouldSetEmptyStateMessage() {
        controller.setEmptyStateMessage("title", "description");

        verify(adapter).setEmptyStateMessage("title", "description");
    }
}
