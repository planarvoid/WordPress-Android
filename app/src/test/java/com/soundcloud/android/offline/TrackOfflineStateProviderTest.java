package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;


public class TrackOfflineStateProviderTest extends AndroidUnitTest {

    private static final Urn REQUESTED_TRACK = Urn.forTrack(1);
    private static final Urn UNAVAILABLE_TRACK = Urn.forTrack(2);
    private static final Urn TRACK_3 = Urn.forTrack(3);

    private TrackOfflineStateProvider trackOfflineStateProvider;

    @Mock private TrackDownloadsStorage trackDownloadsStorage;

    private EventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        trackOfflineStateProvider = new TrackOfflineStateProvider(trackDownloadsStorage,
                                                                  eventBus,
                                                                  Schedulers.immediate());
        when(trackDownloadsStorage.getOfflineStates()).thenReturn(Observable.just(getInitialMap()));
    }

    @Test
    public void providesStatesFromStorage() {
        trackOfflineStateProvider.subscribe();

        assertThat(trackOfflineStateProvider.getOfflineState(REQUESTED_TRACK)).isSameAs(OfflineState.REQUESTED);
        assertThat(trackOfflineStateProvider.getOfflineState(UNAVAILABLE_TRACK)).isSameAs(OfflineState.UNAVAILABLE);
        assertThat(trackOfflineStateProvider.getOfflineState(TRACK_3)).isSameAs(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void providesStatesFromEventBus() {
        trackOfflineStateProvider.subscribe();

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded(Collections.singletonList(REQUESTED_TRACK), true));
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested(Collections.singletonList(UNAVAILABLE_TRACK), true));
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloaded(Collections.singletonList(TRACK_3), false));

        assertThat(trackOfflineStateProvider.getOfflineState(REQUESTED_TRACK)).isSameAs(OfflineState.DOWNLOADED);
        assertThat(trackOfflineStateProvider.getOfflineState(UNAVAILABLE_TRACK)).isSameAs(OfflineState.REQUESTED);
        assertThat(trackOfflineStateProvider.getOfflineState(TRACK_3)).isSameAs(OfflineState.DOWNLOADED);
    }

    @Test
    public void clearClearsExistingStates() {
        trackOfflineStateProvider.subscribe();
        trackOfflineStateProvider.clear();

        assertThat(trackOfflineStateProvider.getOfflineState(REQUESTED_TRACK)).isSameAs(OfflineState.NOT_OFFLINE);
        assertThat(trackOfflineStateProvider.getOfflineState(UNAVAILABLE_TRACK)).isSameAs(OfflineState.NOT_OFFLINE);
        assertThat(trackOfflineStateProvider.getOfflineState(TRACK_3)).isSameAs(OfflineState.NOT_OFFLINE);
    }

    @NonNull
    private HashMap<Urn, OfflineState> getInitialMap() {
        final HashMap<Urn, OfflineState> urnOfflineStateHashMap = new HashMap<>();
        urnOfflineStateHashMap.put(REQUESTED_TRACK, OfflineState.REQUESTED);
        urnOfflineStateHashMap.put(UNAVAILABLE_TRACK, OfflineState.UNAVAILABLE);
        return urnOfflineStateHashMap;
    }
}
