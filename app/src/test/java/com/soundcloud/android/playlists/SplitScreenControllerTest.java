package com.soundcloud.android.playlists;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
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
    @Mock private Resources resources;
    @Mock private View layout;

    @Before
    public void setUp() throws Exception {
        controller = new SplitScreenController(trackPresenter, adapter, eventBus);
        when(layout.findViewById(android.R.id.list)).thenReturn(listView);
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(layout.findViewById(R.id.container)).thenReturn(container);
        controller.onViewCreated(layout, resources);
    }

    @Test
    public void shouldListenForTrackChangeEventsAndUpdateTrackPresenter() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(TrackUrn.forTrack(123L)));

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
