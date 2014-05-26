package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.FrameLayout;

import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class InlinePlaylistTrackPresenterTest {

    private InlinePlaylistTrackPresenter presenter;
    private ViewGroup parent = new FrameLayout(Robolectric.application);

    @Mock
    private ImageOperations imageOperations;
    @Mock
    private EmptyView emptyView;

    @Before
    public void setup() {
        presenter = new InlinePlaylistTrackPresenter(imageOperations);
    }

    @Test
    public void createsEmptyListViewWithNoDataForIgnoredItemType() throws Exception {
        View view = presenter.createItemView(0, parent, Adapter.IGNORE_ITEM_VIEW_TYPE);
        expect(view).toBeInstanceOf(EmptyView.class);
    }

    @Test
    public void bindsEmptyViewWithWaitingStateByDefault() throws Exception {
        presenter.bindItemView(0, emptyView, Collections.<Track>emptyList());
        verify(emptyView).setStatus(EmptyView.Status.WAITING);
    }

    @Test
    public void bindsEmptyViewWithCustomState() throws Exception {
        presenter.setEmptyViewStatus(EmptyView.Status.ERROR);
        presenter.bindItemView(0, emptyView, Collections.<Track>emptyList());
        verify(emptyView).setStatus(EmptyView.Status.ERROR);
    }

    @Test
    public void shouldCreateTrackItemView() throws Exception {
        View view = presenter.createItemView(0, parent, ItemAdapter.DEFAULT_ITEM_VIEW_TYPE);
        expect(view).toBeInstanceOf(PlayableRow.class);
    }

}