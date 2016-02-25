package com.soundcloud.android.playlists;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.widget.ListView;

public class SplitScreenControllerTest extends AndroidUnitTest {

    private SplitScreenController controller;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaylistTrackItemRenderer trackRenderer;
    @Mock private ListView listView;
    @Mock private EmptyView emptyView;
    @Mock private View container;
    @Mock private View layout;

    @Before
    public void setUp() throws Exception {
        controller = new SplitScreenController(trackRenderer, eventBus);
        when(layout.findViewById(android.R.id.list)).thenReturn(listView);
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(layout.findViewById(R.id.container)).thenReturn(container);
        when(layout.getContext()).thenReturn(context());
        controller.onViewCreated(layout, null);
    }

    @Test
    public void shouldListenForPositionChangeEventsAndUpdateTrackPresenter() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), Urn.NOT_SET, 0));

        verify(trackRenderer).setPlayingTrack(Urn.forTrack(123L));
    }

    @Test
    public void shouldListenForNewQueueEventsAndUpdateTrackPresenter() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), Urn.NOT_SET, 0));

        verify(trackRenderer).setPlayingTrack(Urn.forTrack(123L));
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesInOnDestroyView() {
        controller.onDestroyView();

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void setListShownWithTrueSetsListContainerVisibilityToVisible() throws Exception {
        controller.setListShown(true);
        verify(container).setVisibility(View.VISIBLE);
    }

    @Test
    public void setListShownWithFalseSetsListContainerVisibilityToGone() throws Exception {
        controller.setListShown(false);
        verify(container).setVisibility(View.GONE);
    }

    @Test
    public void setEmptyViewStatusSetsStatesOnEmptyView() throws Exception {
        controller.setEmptyViewStatus(EmptyView.Status.OK);
        verify(emptyView).setStatus(EmptyView.Status.OK);
    }

    @Test
    public void shouldDisableClicksForBlockedTracks() {
        TrackItem blockedTrack = TrackItem.from(TestPropertySets.fromApiTrack().put(TrackProperty.BLOCKED, true));
        PropertySet playlist = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        controller.setContent(new PlaylistWithTracks(playlist, singletonList(blockedTrack)), null);

        assertThat(controller.getAdapter().isEnabled(0)).isFalse();
    }
}
