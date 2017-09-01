package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;

public class UpdateCurrentDownloadObserverTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);

    private UpdateCurrentDownloadObserver subscriber;

    @Mock private RecyclerItemAdapter<TrackItem, ?> adapter;

    @Before
    public void setUp() throws Exception {
        subscriber = new UpdateCurrentDownloadObserver(adapter);
    }

    @Test
    public void startEventUpdatesItemWithTheSameUrnAndNotifies() {
        TrackItem track1 = PlayableFixtures.expectedTrackForListItem(TRACK1);
        TrackItem track2 = PlayableFixtures.expectedTrackForListItem(TRACK2);
        final ArrayList<TrackItem> trackItems = newArrayList(track1, track2);
        when(adapter.getItems()).thenReturn(
                trackItems);

        final OfflineContentChangedEvent event = downloading(singletonList(TRACK1), true);
        subscriber.onNext(event);

        assertThat(trackItems.get(0).offlineState()).isEqualTo(OfflineState.DOWNLOADING);
        assertThat(trackItems.get(1).offlineState()).isEqualTo(OfflineState.NOT_OFFLINE);
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void stopEventUpdatesItemWithTheSameUrnAndNotifies() {
        TrackItem track1 = PlayableFixtures.expectedTrackForListItem(TRACK1);
        TrackItem track2 = PlayableFixtures.expectedTrackForListItem(TRACK2);
        final ArrayList<TrackItem> trackItems = newArrayList(track1, track2);
        when(adapter.getItems()).thenReturn(
                trackItems);

        final OfflineContentChangedEvent event = downloaded(singletonList(TRACK1), false);
        subscriber.onNext(event);

        assertThat(trackItems.get(0).offlineState()).isEqualTo(OfflineState.DOWNLOADED);
        assertThat(trackItems.get(1).offlineState()).isEqualTo(OfflineState.NOT_OFFLINE);
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void doesNotNotifyWhenUrnNotPresent() {
        TrackItem track1 = PlayableFixtures.expectedTrackForListItem(TRACK1);
        when(adapter.getItems()).thenReturn(
                newArrayList(track1));

        final OfflineContentChangedEvent event = downloaded(singletonList(TRACK2), false);
        subscriber.onNext(event);

        verify(adapter, never()).notifyItemChanged(anyInt());
    }
}