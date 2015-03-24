package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.PlaylistTrackItemPresenter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.ItemAdapter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistDetailsControllerTest {

    PlaylistDetailsController controller;

    @Mock private PlaylistTrackItemPresenter trackItemPresenter;
    @Mock private ItemAdapter itemAdapter;
    private EventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        controller = new PlaylistDetailsControllerImpl(trackItemPresenter, itemAdapter, eventBus);
    }

    @Test
    public void hasTracksIsFalseIfAdapterDataIsEmpty() throws Exception {
        expect(controller.hasTracks()).toBeFalse();
    }

    @Test
    public void hasTracksIsTrueIfAdapterDataIsNotEmpty() throws Exception {
        when(itemAdapter.getItems()).thenReturn(Arrays.asList(0, 1, 2));
        expect(controller.hasTracks()).toBeTrue();
    }

    private static class PlaylistDetailsControllerImpl extends PlaylistDetailsController {

        protected PlaylistDetailsControllerImpl(PlaylistTrackItemPresenter trackPresenter,
                                                ItemAdapter<TrackItem> adapter, EventBus eventBus) {
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
        public void setEmptyViewStatus(int status) {

        }
    }

}