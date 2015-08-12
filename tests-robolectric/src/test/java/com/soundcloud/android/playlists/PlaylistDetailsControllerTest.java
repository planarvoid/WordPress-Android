package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.view.EmptyView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistDetailsControllerTest {

    PlaylistDetailsController controller;

    @Mock private PlaylistTrackItemRenderer trackItemRenderer;
    @Mock private ListItemAdapter<TrackItem> itemAdapter;
    private EventBus eventBus = new TestEventBus();
    private PlaylistWithTracks playlist;

    @Before
    public void setUp() throws Exception {
        controller = new PlaylistDetailsControllerImpl(trackItemRenderer, itemAdapter, eventBus);
        playlist = createPlaylist();
    }

    @Test
    public void hasTracksIsFalseIfAdapterDataIsEmpty() throws Exception {
        expect(controller.hasTracks()).toBeFalse();
    }

    @Test
    public void hasTracksIsTrueIfAdapterDataIsNotEmpty() throws Exception {
        when(itemAdapter.getItems()).thenReturn(playlist.getTracks());
        expect(controller.hasTracks()).toBeTrue();
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterWhenPlaylistIsReturned() throws Exception {
        controller.setContent(playlist, null);

        InOrder inOrder = Mockito.inOrder(itemAdapter);
        inOrder.verify(itemAdapter).clear();
        for (TrackItem track : playlist.getTracks()) {
            inOrder.verify(itemAdapter).addItem(track);
        }
    }

    private PlaylistWithTracks createPlaylist() {
        return new PlaylistWithTracks(
                ModelFixtures.create(ApiPlaylist.class).toPropertySet(),
                ModelFixtures.trackItems(10));
    }

    private static class PlaylistDetailsControllerImpl extends PlaylistDetailsController {

        protected PlaylistDetailsControllerImpl(PlaylistTrackItemRenderer trackPresenter,
                                                ListItemAdapter<TrackItem> adapter, EventBus eventBus) {
            super(trackPresenter, adapter, eventBus);
        }

        @Override
        boolean hasContent() {
            return false;
        }

        @Override
        void setListShown(boolean show) {

        }

        @Override
        public void setEmptyViewStatus(EmptyView.Status status) {

        }
    }

}