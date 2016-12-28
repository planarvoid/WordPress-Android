package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class UpdateCurrentDownloadSubscriberTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);

    private UpdateCurrentDownloadSubscriber subscriber;

    @Mock private RecyclerItemAdapter<TrackItem, ?> adapter;

    @Before
    public void setUp() throws Exception {
        subscriber = new UpdateCurrentDownloadSubscriber(adapter);
    }

    @Test
    public void startEventUpdatesItemWithTheSameUrnAndNotifies() {
        TrackItem track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        TrackItem track2 = TestPropertySets.expectedTrackForListItem(TRACK2);
        when(adapter.getItems()).thenReturn(
                newArrayList(track1, track2));

        final OfflineContentChangedEvent event = downloading(singletonList(TRACK1), true);
        subscriber.onNext(event);

        assertThat(track1.getOfflineState()).isEqualTo(OfflineState.DOWNLOADING);
        assertThat(track2.getOfflineState()).isEqualTo(OfflineState.NOT_OFFLINE);
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void stopEventUpdatesItemWithTheSameUrnAndNotifies() {
        TrackItem track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        TrackItem track2 = TestPropertySets.expectedTrackForListItem(TRACK2);
        when(adapter.getItems()).thenReturn(
                newArrayList(track1, track2));

        final OfflineContentChangedEvent event = downloaded(singletonList(TRACK1), false);
        subscriber.onNext(event);

        assertThat(track1.getOfflineState()).isEqualTo(OfflineState.DOWNLOADED);
        assertThat(track2.getOfflineState()).isEqualTo(OfflineState.NOT_OFFLINE);
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void doesNotNotifyWhenUrnNotPresent() {
        TrackItem track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        when(adapter.getItems()).thenReturn(
                newArrayList(track1));

        final OfflineContentChangedEvent event = downloaded(singletonList(TRACK2), false);
        subscriber.onNext(event);

        verify(adapter, never()).notifyItemChanged(anyInt());
    }
}
