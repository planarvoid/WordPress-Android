package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

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

        final CurrentDownloadEvent event = CurrentDownloadEvent.start(TRACK1);
        subscriber.onNext(event);

        expect(track1.get(OfflineProperty.DOWNLOADING)).toEqual(true);
        expect(track2.contains(OfflineProperty.DOWNLOADING)).toBeFalse();
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void stopEventUpdatesItemWithTheSameUrnAndNotifies() {
        PropertySet track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        PropertySet track2 = TestPropertySets.expectedTrackForListItem(TRACK2);
        when(adapter.getItems()).thenReturn(
                Lists.newArrayList(TrackItem.from(track1), TrackItem.from(track2)));

        final CurrentDownloadEvent event = CurrentDownloadEvent.stop(TRACK1);
        subscriber.onNext(event);

        expect(track1.get(OfflineProperty.DOWNLOADING)).toEqual(false);
        expect(track2.contains(OfflineProperty.DOWNLOADING)).toBeFalse();
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void doesNotNotifyWhenUrnNotPresent() {
        PropertySet track1 = TestPropertySets.expectedTrackForListItem(TRACK1);
        when(adapter.getItems()).thenReturn(
                Lists.newArrayList(TrackItem.from(track1)));

        final CurrentDownloadEvent event = CurrentDownloadEvent.start(TRACK2);
        subscriber.onNext(event);

        verify(adapter, never()).notifyDataSetChanged();
    }
}