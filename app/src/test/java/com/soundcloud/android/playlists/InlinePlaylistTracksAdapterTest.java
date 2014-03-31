package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.view.EmptyListView.Status;

import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
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

    @Before
    public void setUp() throws Exception {
        adapter = new InlinePlaylistTracksAdapter(imageOperations);
    }

    @Test
    public void reports2DifferentItemTypes() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void hasCountOf1WithNoDataAndInlineEmptyViews() throws Exception {
        expect(adapter.getCount()).toBe(1);
    }

    @Test
    public void getViewCreatesEmptyListViewWithNoData() throws Exception {
        View view = adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(view).toBeInstanceOf(EmptyListView.class);
    }

    @Test
    public void getViewReturnsEmptyListViewWithWaitingStateByDefault() throws Exception {
        EmptyListView view = (EmptyListView) adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(view.getStatus()).toEqual(Status.WAITING);
    }

    @Test
    public void getViewReturnsEmptyListViewWithErrorState() throws Exception {
        adapter.setEmptyViewStatus(Status.ERROR);
        EmptyListView view = (EmptyListView) adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(view.getStatus()).toEqual(Status.ERROR);
    }

    @Test
    public void getViewReturnsEmptyListViewWithOkStateAndNoItemsOnAdapter() throws Exception {
        adapter.setEmptyViewStatus(Status.OK);
        EmptyListView view = (EmptyListView) adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(view.getStatus()).toEqual(Status.OK);
    }

    @Test
    public void returnTrackRowWithData() throws Exception {
        adapter.addItem(new Track(1));
        expect(adapter.getView(0, null, new FrameLayout(Robolectric.application))).toBeInstanceOf(PlayableRow.class);
    }

    @Test
    public void hadContentItemsShouldBeFalseWhenErrorStateIsShown() {
        adapter.setEmptyViewStatus(Status.ERROR);
        expect(adapter.hasContentItems()).toBeFalse();
    }

    @Test
    public void hasContentItemsShouldBeFalseWhenNoItemsHaveBeenSet() {
        expect(adapter.hasContentItems()).toBeFalse();
    }

    @Test
    public void hasContentItemsShouldBeTrueOnceItemsHaveBeenAdded() {
        adapter.addItem(new Track(1));
        expect(adapter.hasContentItems()).toBeTrue();
    }

    @Test
    public void hasContentItemsShouldBeTrueWhenItemsAddedAfterError() {
        adapter.setEmptyViewStatus(Status.ERROR);
        adapter.addItem(new Track(1));
        expect(adapter.hasContentItems()).toBeTrue();
    }

}
