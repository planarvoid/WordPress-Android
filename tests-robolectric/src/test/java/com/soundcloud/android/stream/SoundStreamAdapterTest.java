package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamAdapterTest {

    private SoundStreamAdapter adapter;

    @Mock private TrackItemPresenter trackItemPresenter;
    @Mock private PlaylistItemPresenter playlistItemPresenter;

    @Before
    public void setup() {
        adapter = new SoundStreamAdapter(trackItemPresenter, playlistItemPresenter);
    }

    @Test
    public void hasTwoDifferentItemViewTypes() {
        expect(adapter.getViewTypeCount()).toEqual(2);
    }

    @Test
    public void returnsTrackTypeForTracks() {
        adapter.addItem(ModelFixtures.create(TrackItem.class));
        expect(adapter.getItemViewType(0)).toEqual(SoundStreamAdapter.TRACK_ITEM_TYPE);
    }

    @Test
    public void returnsPlaylistTypeForPlaylists() {
        adapter.addItem(ModelFixtures.create(PlaylistItem.class));
        expect(adapter.getItemViewType(0)).toEqual(SoundStreamAdapter.PLAYLIST_ITEM_TYPE);
    }

}