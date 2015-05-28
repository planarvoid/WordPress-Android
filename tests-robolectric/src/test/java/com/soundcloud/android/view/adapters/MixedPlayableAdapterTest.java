package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class MixedPlayableAdapterTest {

    private MixedPlayableAdapter adapter;

    @Mock private TrackItemRenderer trackItemRenderer;
    @Mock private PlaylistItemRenderer playlistItemRenderer;

    @Before
    public void setup() {
        adapter = new MixedPlayableAdapter(trackItemRenderer, playlistItemRenderer);
    }

    @Test
    public void hasTwoDifferentItemViewTypes() {
        expect(adapter.getViewTypeCount()).toEqual(2);
    }

    @Test
    public void returnsTrackTypeForTracks() {
        adapter.addItem(ModelFixtures.create(TrackItem.class));
        expect(adapter.getItemViewType(0)).toEqual(MixedPlayableAdapter.TRACK_ITEM_TYPE);
    }

    @Test
    public void returnsPlaylistTypeForPlaylists() {
        adapter.addItem(ModelFixtures.create(PlaylistItem.class));
        expect(adapter.getItemViewType(0)).toEqual(MixedPlayableAdapter.PLAYLIST_ITEM_TYPE);
    }

}