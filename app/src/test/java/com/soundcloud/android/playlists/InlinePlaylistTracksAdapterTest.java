package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.view.EmptyListView.Status;

import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.widget.FrameLayout;

@RunWith(SoundCloudTestRunner.class)
public class InlinePlaylistTracksAdapterTest {

    private InlinePlaylistTracksAdapter adapter;

    @Mock
    private ImageOperations imageOperations;

    @Test
    public void reports2DifferentItemTypes() {
        adapter = new InlinePlaylistTracksAdapter(imageOperations);
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void hasCountOf1WithNoDataAndInlineEmptyViews() throws Exception {
        adapter = new InlinePlaylistTracksAdapter(imageOperations);
        expect(adapter.getCount()).toBe(1);
    }


    @Test
    public void getViewCreatesEmptyListViewWithNoData() throws Exception {
        adapter = new InlinePlaylistTracksAdapter(imageOperations);
        View view = adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(view).toBeInstanceOf(EmptyListView.class);
    }

    @Test
    public void getViewReturnsEmptyListViewWithWaitingStateByDefault() throws Exception {
        adapter = new InlinePlaylistTracksAdapter(imageOperations);
        EmptyListView view = (EmptyListView) adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(view.getStatus()).toEqual(Status.WAITING);
    }

    @Test
    public void getViewReturnsEmptyListViewWithErrorState() throws Exception {
        adapter = new InlinePlaylistTracksAdapter(imageOperations);
        adapter.setEmptyViewStatus(Status.ERROR);
        EmptyListView view = (EmptyListView) adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(view.getStatus()).toEqual(Status.ERROR);
    }

    @Test
    public void getViewReturnsEmptyListViewWithOkStateAndNoItemsOnAdapter() throws Exception {
        adapter = new InlinePlaylistTracksAdapter(imageOperations);
        adapter.setEmptyViewStatus(Status.OK);
        EmptyListView view = (EmptyListView) adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(view.getStatus()).toEqual(Status.OK);
    }

    @Test
    public void returnTrackRowWithData() throws Exception {
        adapter = new InlinePlaylistTracksAdapter(imageOperations);
        adapter.addItem(new Track(1L));

        expect(adapter.getView(0, null, new FrameLayout(Robolectric.application))).toBeInstanceOf(PlayableRow.class);
    }
}
