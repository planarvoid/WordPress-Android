package com.soundcloud.android.view.adapters;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class UpdatePlaylistListSubscriberTest  extends AndroidUnitTest {

    private UpdatePlaylistListSubscriber updatePlaylistListSubscriber;

    @Mock private RecyclerItemAdapter<PlaylistItem, RecyclerView.ViewHolder> adapter;
    private final int UPDATED_TRACK_COUNT = 10;

    @Before
    public void setUp() throws Exception {
        updatePlaylistListSubscriber = new UpdatePlaylistListSubscriber(adapter);
    }

    @Test
    public void updatesItemWithTheSameUrnAndNotifies() {
        PlaylistItem playlists1 = ModelFixtures.playlistItemBuilder().trackCount(5).build();
        PlaylistItem playlists2 = ModelFixtures.playlistItem();

        final List<PlaylistItem> items = Arrays.asList(playlists1, playlists2);
        when(adapter.getItems()).thenReturn(items);

        final PlaylistChangedEvent event = PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist(playlists1.getUrn(), UPDATED_TRACK_COUNT);
        updatePlaylistListSubscriber.onNext(event);

        final PlaylistItem updatedItem = items.get(0);
        assertThat(updatedItem.getUrn()).isEqualTo(playlists1.getUrn());
        assertThat(updatedItem.getTrackCount()).isEqualTo(UPDATED_TRACK_COUNT);
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void doesNotNotifyWithNoMatchingUrns() {
        PlaylistItem playlists1 = ModelFixtures.playlistItem();
        PlaylistItem playlists2 = ModelFixtures.playlistItem();

        ApiPlaylist changeSet = ModelFixtures.create(ApiPlaylist.class);

        when(adapter.getItems()).thenReturn(newArrayList(playlists1, playlists2));

        final PlaylistChangedEvent event = PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist(changeSet.getUrn(), UPDATED_TRACK_COUNT);
        updatePlaylistListSubscriber.onNext(event);

        verify(adapter, never()).notifyItemChanged(anyInt());
    }
}
