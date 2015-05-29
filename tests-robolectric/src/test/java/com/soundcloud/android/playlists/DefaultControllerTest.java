package com.soundcloud.android.playlists;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.view.View;
import android.widget.ListView;

@RunWith(SoundCloudTestRunner.class)
public class DefaultControllerTest {

    private DefaultController controller;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaylistTrackItemRenderer trackRenderer;
    @Mock private InlinePlaylistTracksAdapter adapter;
    @Mock private ListView listView;
    @Mock private Resources resources;
    @Mock private View layout;

    @Before
    public void setUp() throws Exception {
        when(layout.findViewById(android.R.id.list)).thenReturn(listView);
        when(adapter.getPlaylistItemRenderer()).thenReturn(trackRenderer);
        controller = new DefaultController(adapter, eventBus);
        controller.onViewCreated(layout, null);
    }

    @Test
    public void shouldListenForTrackPositionChangeEventsAndUpdateTrackPresenter() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.forTrack(123L)));

        verify(trackRenderer).setPlayingTrack(Urn.forTrack(123L));
    }

    @Test
    public void shouldListenForNewPlayQueueEventsAndUpdateTrackPresenter() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(Urn.forTrack(123L)));

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
        controller.setEmptyViewStatus(100);
        verify(adapter).setEmptyViewStatus(100);
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void hasContentShouldCheckInternalAdapterItemCount() {
        controller.hasContent();
        verify(adapter).hasContentItems();
    }

}
