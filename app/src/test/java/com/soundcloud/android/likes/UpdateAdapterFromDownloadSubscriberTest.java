package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class UpdateAdapterFromDownloadSubscriberTest {

    private UpdateAdapterFromDownloadSubscriber subscriber;

    @Mock private ItemAdapter<PropertySet> adapter;

    private final PropertySet track1 = TestPropertySets.fromApiTrack();
    private final PropertySet track2 = TestPropertySets.fromApiTrack();

    @Before
    public void setUp() throws Exception {
        subscriber = new UpdateAdapterFromDownloadSubscriber(adapter);
    }

    @Test
    public void downloadingStartedEventUpdatesDownloadTimeOfMatchingTrack() throws Exception {
        when(adapter.getItems()).thenReturn(Arrays.asList(track1, track2));

        subscriber.onNext(OfflineContentEvent.downloadStarted(track1.get(EntityProperty.URN)));

        expect(track1.get(TrackProperty.OFFLINE_DOWNLOADED_AT)).not.toBeNull();
        expect(track2.contains(TrackProperty.OFFLINE_DOWNLOADED_AT)).toBeFalse();
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void downloadingStartedEventDoesUpdatesDownloadTimeOfAnyTrackWithUnmatchedUrl() throws Exception {
        when(adapter.getItems()).thenReturn(Arrays.asList(track1, track2));

        subscriber.onNext(OfflineContentEvent.downloadStarted(Urn.forTrack(123L)));

        expect(track1.contains(TrackProperty.OFFLINE_DOWNLOADED_AT)).toBeFalse();
        expect(track2.contains(TrackProperty.OFFLINE_DOWNLOADED_AT)).toBeFalse();
        verify(adapter, never()).notifyDataSetChanged();
    }

    @Test
    public void offlineProgressEventDoesUpdatesDownloadTimeOfAnyTrackWithWrongEventType() throws Exception {
        when(adapter.getItems()).thenReturn(Arrays.asList(track1, track2));

        subscriber.onNext(OfflineContentEvent.idle());
        subscriber.onNext(OfflineContentEvent.start());
        subscriber.onNext(OfflineContentEvent.stop());

        expect(track1.contains(TrackProperty.OFFLINE_DOWNLOADED_AT)).toBeFalse();
        expect(track2.contains(TrackProperty.OFFLINE_DOWNLOADED_AT)).toBeFalse();
        verify(adapter, never()).notifyDataSetChanged();
    }
}