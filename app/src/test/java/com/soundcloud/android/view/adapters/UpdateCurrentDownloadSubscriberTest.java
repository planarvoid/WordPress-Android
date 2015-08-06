package com.soundcloud.android.view.adapters;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

public class UpdateCurrentDownloadSubscriberTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);

    private UpdateCurrentDownloadSubscriber subscriber;

    @Mock private ItemAdapter<TrackItem> adapter;

    @Before
    public void setUp() throws Exception {
        subscriber = new UpdateCurrentDownloadSubscriber(adapter);
    }

    @Test
    public void startEventUpdatesItemWithTheSameUrnAndNotifies() {
        PropertySet track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        PropertySet track2 = TestPropertySets.expectedTrackForListItem(TRACK2);
        when(adapter.getItems()).thenReturn(
                newArrayList(TrackItem.from(track1), TrackItem.from(track2)));

        final DownloadRequest request = new DownloadRequest.Builder(TRACK1, 12345L, "http://", true).build();
        final CurrentDownloadEvent event = CurrentDownloadEvent.downloading(request);
        subscriber.onNext(event);

        assertThat(track1.get(OfflineProperty.OFFLINE_STATE)).isEqualTo(OfflineState.DOWNLOADING);
        assertThat(track2.contains(OfflineProperty.OFFLINE_STATE)).isFalse();
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void stopEventUpdatesItemWithTheSameUrnAndNotifies() {
        PropertySet track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        PropertySet track2 = TestPropertySets.expectedTrackForListItem(TRACK2);
        when(adapter.getItems()).thenReturn(
                newArrayList(TrackItem.from(track1), TrackItem.from(track2)));

        final CurrentDownloadEvent event = CurrentDownloadEvent.downloaded(false, Collections.singletonList(TRACK1));
        subscriber.onNext(event);

        assertThat(track1.get(OfflineProperty.OFFLINE_STATE)).isEqualTo(OfflineState.DOWNLOADED);
        assertThat(track2.contains(OfflineProperty.OFFLINE_STATE)).isFalse();
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void doesNotNotifyWhenUrnNotPresent() {
        PropertySet track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        when(adapter.getItems()).thenReturn(
                newArrayList(TrackItem.from(track1)));

        final CurrentDownloadEvent event = CurrentDownloadEvent.downloaded(false, Collections.singletonList(TRACK2));
        subscriber.onNext(event);

        verify(adapter, never()).notifyDataSetChanged();
    }
}