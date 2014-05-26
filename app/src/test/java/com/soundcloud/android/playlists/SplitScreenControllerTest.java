package com.soundcloud.android.playlists;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.ItemAdapter;
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

    @Mock
    private ItemAdapter<Track, PlayableRow> adapter;
    @Mock
    private ListView listView;
    @Mock
    private EmptyView emptyView;
    @Mock
    private View container;
    @Mock
    private Resources resources;
    @Mock
    private View layout;


    @Before
    public void setUp() throws Exception {
        controller = new SplitScreenController(adapter);
        when(layout.findViewById(android.R.id.list)).thenReturn(listView);
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(layout.findViewById(R.id.container)).thenReturn(container);
        controller.onViewCreated(layout, resources);
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
