package com.soundcloud.android.playlists;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.widget.ListView;

@RunWith(SoundCloudTestRunner.class)
public class SplitScreenControllerTest {

    private SplitScreenController controller;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private TrackItemPresenter trackPresenter;
    @Mock private ItemAdapter<PropertySet> adapter;
    @Mock private ListView listView;
    @Mock private EmptyView emptyView;
    @Mock private View container;
    @Mock private View layout;

    @Before
    public void setUp() throws Exception {
        controller = new SplitScreenController(trackPresenter, adapter, eventBus);
        when(layout.findViewById(android.R.id.list)).thenReturn(listView);
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(layout.findViewById(R.id.container)).thenReturn(container);
        when(layout.getContext()).thenReturn(Robolectric.application);
        controller.onViewCreated(layout, null);
    }

    @Test
    public void shouldListenForPositionChangeEventsAndUpdateTrackPresenter() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.forTrack(123L)));

        verify(trackPresenter).setPlayingTrack(TrackUrn.forTrack(123L));
    }

    @Test
    public void shouldListenForNewQueueEventsAndUpdateTrackPresenter() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TrackUrn.forTrack(123L)));

        verify(trackPresenter).setPlayingTrack(TrackUrn.forTrack(123L));
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
        controller.setEmptyViewStatus(100);
        verify(emptyView).setStatus(100);
    }
}
