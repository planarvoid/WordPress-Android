package com.soundcloud.android.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Date;

public class LikeStreamEntitySubscriberTest extends AndroidUnitTest {

    @Mock StreamAdapter mockAdapter;
    @Captor ArgumentCaptor<StreamItem> streamItemArgumentCaptor;

    private LikeStreamEntitySubscriber testSubscriber;

    @Test
    public void updatesTrackWithMatchingUrn() throws Exception {
        final TrackStreamItem trackStreamItem1 = initTrackStreamItem();
        final TrackStreamItem trackStreamItem2 = initTrackStreamItem();

        testSubscriber = new LikeStreamEntitySubscriber(mockAdapter);
        initAdapter(Lists.newArrayList(trackStreamItem1, trackStreamItem2));

        final int likesCount = 10;
        final LikesStatusEvent likeEvent = LikesStatusEvent.create(trackStreamItem1.trackItem().getUrn(), true, likesCount);

        testSubscriber.onNext(likeEvent);

        verify(mockAdapter).setItem(eq(0), streamItemArgumentCaptor.capture());
        verify(mockAdapter, never()).setItem(eq(1), any(StreamItem.class));
        final TrackItem updatedTrackItem = (TrackItem) streamItemArgumentCaptor.getValue().getListItem().get();
        assertThat(updatedTrackItem.isLiked()).isTrue();
        assertThat(updatedTrackItem.getLikesCount()).isEqualTo(likesCount);
    }

    @Test
    public void updatesPlaylistWithMatchingUrn() throws Exception {
        final StreamItem.Playlist playlistStreamItem1 = initPlaylistStreamItem();
        final StreamItem.Playlist playlistStreamItem2 = initPlaylistStreamItem();

        testSubscriber = new LikeStreamEntitySubscriber(mockAdapter);
        initAdapter(Lists.newArrayList(playlistStreamItem1, playlistStreamItem2));

        final int likesCount = 10;
        final LikesStatusEvent likeEvent = LikesStatusEvent.create(playlistStreamItem1.playlistItem().getUrn(), true, likesCount);

        testSubscriber.onNext(likeEvent);

        verify(mockAdapter).setItem(eq(0), streamItemArgumentCaptor.capture());
        verify(mockAdapter, never()).setItem(eq(1), any(StreamItem.class));
        final PlaylistItem updatedPlaylistItem = (PlaylistItem) streamItemArgumentCaptor.getValue().getListItem().get();
        assertThat(updatedPlaylistItem.isLiked()).isTrue();
        assertThat(updatedPlaylistItem.getLikesCount()).isEqualTo(likesCount);
    }

    private void initAdapter(ArrayList<StreamItem> streamItems) {
        when(mockAdapter.getItems()).thenReturn(streamItems);
        for (int position = 0; position < streamItems.size(); position++) {
            when(mockAdapter.getItem(position)).thenReturn(streamItems.get(position));
        }
    }

    private TrackStreamItem initTrackStreamItem() {
        final TrackItem trackItem = ModelFixtures.trackItem();
        trackItem.getSource().put(PlayableProperty.LIKES_COUNT, 0);
        trackItem.getSource().put(PlayableProperty.IS_USER_LIKE, false);
        return TrackStreamItem.create(trackItem, new Date());
    }

    private StreamItem.Playlist initPlaylistStreamItem() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        playlistItem.getSource().put(PlayableProperty.LIKES_COUNT, 0);
        playlistItem.getSource().put(PlayableProperty.IS_USER_LIKE, false);
        return StreamItem.Playlist.create(playlistItem, new Date());
    }
}
