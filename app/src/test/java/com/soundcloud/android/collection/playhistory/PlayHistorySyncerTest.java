package com.soundcloud.android.collection.playhistory;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PlayHistorySyncerTest {
    private static final PlayHistoryRecord EXISTING = PlayHistoryRecord.create(1000L, Urn.forTrack(1L), Urn.NOT_SET);
    private static final PlayHistoryRecord MISSING = PlayHistoryRecord.create(2000L, Urn.forTrack(2L), Urn.NOT_SET);
    private static final PlayHistoryRecord REMOVED = PlayHistoryRecord.create(3000L, Urn.forTrack(3L), Urn.NOT_SET);

    private static final List<PlayHistoryRecord> LOCAL = Arrays.asList(EXISTING, REMOVED);
    private static final List<PlayHistoryRecord> REMOTE = Arrays.asList(EXISTING, MISSING);

    @Mock private PlayHistoryStorage playHistoryStorage;
    @Mock private FetchPlayHistoryCommand fetchPlayHistoryCommand;
    @Mock private PushPlayHistoryCommand pushPlayHistoryCommand;
    @Mock private FetchTracksCommand fetchTracksCommand;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private OptimizePlayHistoryCommand optimizePlayHistoryCommand;
    @Mock private EventBus eventBus;

    private PlayHistorySyncer syncer;

    @Before
    public void setUp() throws Exception {
        when(playHistoryStorage.loadSyncedPlayHistory()).thenReturn(LOCAL);
        when(fetchPlayHistoryCommand.call()).thenReturn(REMOTE);

        syncer = new PlayHistorySyncer(playHistoryStorage, fetchPlayHistoryCommand, pushPlayHistoryCommand,
                                       fetchTracksCommand, storeTracksCommand, eventBus, optimizePlayHistoryCommand);
    }

    @Test
    public void shouldSyncExistingPlayHistory() throws Exception {
        syncer.call();

        verify(playHistoryStorage).insertPlayHistory(singletonList(MISSING));
        verify(playHistoryStorage).removePlayHistory(singletonList(REMOVED));
    }

    @Test
    public void shouldPreloadNewPlayHistory() throws Exception {
        List<Urn> trackUrns = singletonList(MISSING.trackUrn());
        List<ApiTrack> tracks = singletonList(ModelFixtures.create(ApiTrack.class));
        when(fetchTracksCommand.call()).thenReturn(tracks);

        syncer.call();

        verify(fetchTracksCommand).with(trackUrns).call();
        verify(storeTracksCommand).call(tracks);
    }

    @Test
    public void shouldPushUnSyncedPlayHistory() throws Exception {
        syncer.call();

        verify(pushPlayHistoryCommand).call();
    }

    @Test
    public void shouldOptimizeTable() throws Exception {
        syncer.call();

        verify(optimizePlayHistoryCommand).call(1000);
    }

}
