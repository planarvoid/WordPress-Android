package com.soundcloud.android.playlists;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.EmptyView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Collections;

public class EmptyPlaylistTracksRendererTest extends AndroidUnitTest {

    private EmptyPlaylistTracksRenderer renderer;
    private ViewGroup parent = new FrameLayout(context());

    @Mock
    private EmptyView emptyView;

    @Before
    public void setup() {
        renderer = new EmptyPlaylistTracksRenderer();
    }

    @Test
    public void createsEmptyListViewWithNoDataForIgnoredItemType() {
        View view = renderer.createItemView(parent);
        assertThat(view).isInstanceOf(EmptyView.class);
    }

    @Test
    public void bindsEmptyViewWithWaitingStateByDefault() {
        renderer.bindItemView(0, emptyView, Collections.emptyList());
        verify(emptyView).setStatus(EmptyView.Status.WAITING);
    }

    @Test
    public void bindsEmptyViewWithCustomState() {
        renderer.setEmptyViewStatus(EmptyView.Status.ERROR);
        renderer.bindItemView(0, emptyView, Collections.emptyList());
        verify(emptyView).setStatus(EmptyView.Status.ERROR);
    }
}
