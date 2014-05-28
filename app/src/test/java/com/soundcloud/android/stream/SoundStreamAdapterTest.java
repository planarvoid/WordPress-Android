package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamAdapterTest {

    @InjectMocks
    private SoundStreamAdapter adapter;

    @Mock
    private TrackItemPresenter trackItemPresenter;
    @Mock
    private PlaylistItemPresenter playlistItemPresenter;

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        expect(adapter.getViewTypeCount()).toEqual(2);
    }

    @Test
    public void shouldReportTrackTypeForTracks() {
        adapter.addItem(PropertySet.from(PlayableProperty.URN.bind(Urn.forTrack(123))));
        expect(adapter.getItemViewType(0)).toEqual(SoundStreamAdapter.TRACK_ITEM_TYPE);
    }

    @Test
    public void shouldReportPlaylistTypeForPlaylists() {
        adapter.addItem(PropertySet.from(PlayableProperty.URN.bind(Urn.forPlaylist(123))));
        expect(adapter.getItemViewType(0)).toEqual(SoundStreamAdapter.PLAYLIST_ITEM_TYPE);
    }
}