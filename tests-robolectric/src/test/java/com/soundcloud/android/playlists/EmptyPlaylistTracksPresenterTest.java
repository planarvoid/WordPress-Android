package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class EmptyPlaylistTracksPresenterTest {

    private EmptyPlaylistTracksPresenter presenter;
    private ViewGroup parent = new FrameLayout(Robolectric.application);

    @Mock
    private EmptyView emptyView;

    @Before
    public void setup() {
        presenter = new EmptyPlaylistTracksPresenter();
    }

    @Test
    public void createsEmptyListViewWithNoDataForIgnoredItemType() throws Exception {
        View view = presenter.createItemView(parent);
        expect(view).toBeInstanceOf(EmptyView.class);
    }

    @Test
    public void bindsEmptyViewWithWaitingStateByDefault() throws Exception {
        presenter.bindItemView(0, emptyView, Collections.<TrackItem>emptyList());
        verify(emptyView).setStatus(EmptyView.Status.WAITING);
    }

    @Test
    public void bindsEmptyViewWithCustomState() throws Exception {
        presenter.setEmptyViewStatus(EmptyView.Status.ERROR);
        presenter.bindItemView(0, emptyView, Collections.<TrackItem>emptyList());
        verify(emptyView).setStatus(EmptyView.Status.ERROR);
    }
}