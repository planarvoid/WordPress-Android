package com.soundcloud.android.playlists;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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

    @Mock
    private InlinePlaylistTracksAdapter adapter;
    @Mock
    private InlinePlaylistTrackPresenter presenter;
    @Mock
    private ListView listView;
    @Mock
    private Resources resources;
    @Mock
    private View layout;

    @Before
    public void setUp() throws Exception {
        controller = new DefaultController(adapter, presenter);
        when(layout.findViewById(android.R.id.list)).thenReturn(listView);
        controller.onViewCreated(layout, resources);
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
        verify(presenter).setEmptyViewStatus(100);
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void hasContentShouldCheckInternalAdapterItemCount() {
        controller.hasContent();
        verify(adapter).hasContentItems();
    }

}
