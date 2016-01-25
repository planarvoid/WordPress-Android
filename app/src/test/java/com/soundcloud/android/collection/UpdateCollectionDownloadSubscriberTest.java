package com.soundcloud.android.collection;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

public class UpdateCollectionDownloadSubscriberTest extends AndroidUnitTest {

    private static final CollectionItem PREVIEW = CollectionItem.fromCollectionsPreview(
            LikesItem.fromUrns(Collections.singletonList(Urn.forTrack(123L))), Collections.<Urn>emptyList());

    private UpdateCollectionDownloadSubscriber subscriber;

    @Mock private CollectionAdapter adapter;

    @Before
    public void setUp() throws Exception {
        when(adapter.getItems()).thenReturn(singletonList(PREVIEW));
        subscriber = new UpdateCollectionDownloadSubscriber(adapter);
    }

    @Test
    public void likesOfflineChangeEventUpdatesItemAndNotifies() {
        final OfflineContentChangedEvent event = downloading(Collections.<Urn>emptyList(), true);
        subscriber.onNext(event);

        assertThat(PREVIEW.getLikes().getDownloadState()).isEqualTo(OfflineState.DOWNLOADING);
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void doesNotNotifyWhenUrnNotLikesOfflineChange() {
        final OfflineContentChangedEvent event = downloaded(singletonList(Urn.forTrack(234l)), false);
        subscriber.onNext(event);

        verify(adapter, never()).notifyDataSetChanged();
    }

}
