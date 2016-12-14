package com.soundcloud.android.collection;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketRenderer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

public class UpdateCollectionDownloadSubscriberTest extends AndroidUnitTest {

    private static final CollectionItem PREVIEW =
            PreviewCollectionItem.forLikesPlaylistsAndStations(
                    LikesItem.fromTrackPreviews(singletonList(
                            LikedTrackPreview.create(Urn.forTrack(123L), "http://image-url"))),
                    Collections.emptyList(), Collections.emptyList());

    private UpdateCollectionDownloadSubscriber subscriber;

    @Mock private CollectionAdapter adapter;
    @Mock private RecentlyPlayedBucketRenderer recentlyPlayedBucketRenderer;
    @Captor private ArgumentCaptor<CollectionItem> collectionItemArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        when(adapter.getRecentlyPlayedBucketRenderer()).thenReturn(recentlyPlayedBucketRenderer);
        final List<CollectionItem> collectionItems = singletonList(PREVIEW);
        when(adapter.getItems()).thenReturn(collectionItems);
        when(adapter.getItem(0)).thenReturn(collectionItems.get(0));
        subscriber = new UpdateCollectionDownloadSubscriber(adapter);
    }

    @Test
    public void likesOfflineChangeEventUpdatesItemAndNotifies() {
        final OfflineContentChangedEvent event = downloading(Collections.emptyList(), true);
        subscriber.onNext(event);

        verify(adapter).setItem(eq(0), collectionItemArgumentCaptor.capture());
        assertThat(((PreviewCollectionItem)collectionItemArgumentCaptor.getValue()).getLikes().offlineState()).isEqualTo(OfflineState.DOWNLOADING);
    }

    @Test
    public void doesNotNotifyWhenUrnNotLikesOfflineChange() {
        final OfflineContentChangedEvent event = downloaded(singletonList(Urn.forTrack(234L)), false);
        subscriber.onNext(event);

        verify(adapter, never()).notifyDataSetChanged();
    }

}
