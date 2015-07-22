package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class UpdateCurrentDownloadSubscriberTest {

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
                Lists.newArrayList(TrackItem.from(track1), TrackItem.from(track2)));

        final DownloadRequest request = new DownloadRequest.Builder(TRACK1, 12345L).build();
        final CurrentDownloadEvent event = CurrentDownloadEvent.downloading(request);
        subscriber.onNext(event);

        expect(track1.get(OfflineProperty.OFFLINE_STATE)).toEqual(OfflineState.DOWNLOADING);
        expect(track2.contains(OfflineProperty.OFFLINE_STATE)).toBeFalse();
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void stopEventUpdatesItemWithTheSameUrnAndNotifies() {
        PropertySet track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        PropertySet track2 = TestPropertySets.expectedTrackForListItem(TRACK2);
        when(adapter.getItems()).thenReturn(
                Lists.newArrayList(TrackItem.from(track1), TrackItem.from(track2)));

        final CurrentDownloadEvent event = CurrentDownloadEvent.downloaded(false, Arrays.asList(TRACK1));
        subscriber.onNext(event);

        expect(track1.get(OfflineProperty.OFFLINE_STATE)).toEqual(OfflineState.DOWNLOADED);
        expect(track2.contains(OfflineProperty.OFFLINE_STATE)).toBeFalse();
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void doesNotNotifyWhenUrnNotPresent() {
        PropertySet track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        when(adapter.getItems()).thenReturn(
                Lists.newArrayList(TrackItem.from(track1)));

        final CurrentDownloadEvent event = CurrentDownloadEvent.downloaded(false, Arrays.asList(TRACK2));
        subscriber.onNext(event);

        verify(adapter, never()).notifyDataSetChanged();
    }
}