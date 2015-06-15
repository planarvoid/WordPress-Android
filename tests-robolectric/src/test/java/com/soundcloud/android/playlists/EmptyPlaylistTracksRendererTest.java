package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class EmptyPlaylistTracksRendererTest {

    private EmptyPlaylistTracksRenderer renderer;
    private ViewGroup parent = new FrameLayout(Robolectric.application);

    @Mock
    private EmptyView emptyView;

    @Before
    public void setup() {
        renderer = new EmptyPlaylistTracksRenderer();
    }

    @Ignore // RL1 doesn't support dealing with resources from AARs
    @Test
    public void createsEmptyListViewWithNoDataForIgnoredItemType() throws Exception {
        View view = renderer.createItemView(parent);
        expect(view).toBeInstanceOf(EmptyView.class);
    }

    @Test
    public void bindsEmptyViewWithWaitingStateByDefault() throws Exception {
        renderer.bindItemView(0, emptyView, Collections.<TrackItem>emptyList());
        verify(emptyView).setStatus(EmptyView.Status.WAITING);
    }

    @Test
    public void bindsEmptyViewWithCustomState() throws Exception {
        renderer.setEmptyViewStatus(EmptyView.Status.ERROR);
        renderer.bindItemView(0, emptyView, Collections.<TrackItem>emptyList());
        verify(emptyView).setStatus(EmptyView.Status.ERROR);
    }
}