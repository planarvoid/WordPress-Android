package com.soundcloud.android.collection;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketRenderer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

public class UpdateCollectionDownloadSubscriberTest extends AndroidUnitTest {

    private static final CollectionItem PREVIEW =
            PreviewCollectionItem.forLikesPlaylistsAndStations(
                    LikesItem.fromTrackPreviews(singletonList(
                            LikedTrackPreview.create(Urn.forTrack(123L), "http://image-url"))),
                    Collections.<PlaylistItem>emptyList(), Collections.<StationRecord>emptyList());

    private UpdateCollectionDownloadSubscriber subscriber;

    @Mock private CollectionAdapter adapter;
    @Mock private RecentlyPlayedBucketRenderer recentlyPlayedBucketRenderer;

    @Before
    public void setUp() throws Exception {
        when(adapter.getRecentlyPlayedBucketRenderer()).thenReturn(recentlyPlayedBucketRenderer);
        when(adapter.getItems()).thenReturn(singletonList(PREVIEW));
        subscriber = new UpdateCollectionDownloadSubscriber(adapter);
    }

    @Test
    public void likesOfflineChangeEventUpdatesItemAndNotifies() {
        final OfflineContentChangedEvent event = downloading(Collections.<Urn>emptyList(), true);
        subscriber.onNext(event);

        assertThat(((PreviewCollectionItem)PREVIEW).getLikes().getDownloadState()).isEqualTo(OfflineState.DOWNLOADING);
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void doesNotNotifyWhenUrnNotLikesOfflineChange() {
        final OfflineContentChangedEvent event = downloaded(singletonList(Urn.forTrack(234L)), false);
        subscriber.onNext(event);

        verify(adapter, never()).notifyDataSetChanged();
    }

}
