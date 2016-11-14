package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.ResultReceiver;

public class SinglePlaylistJobRequestTest extends AndroidUnitTest {
    private final String ACTION = "action";
    private final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    @Mock private DefaultSyncJob syncJob;
    @Mock private ResultReceiver resultReceiver;

    private SinglePlaylistJobRequest singleJobRequest;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        singleJobRequest = new SinglePlaylistJobRequest(syncJob, ACTION, true, resultReceiver, eventBus, PLAYLIST_URN);
    }

    @Test
    public void finishSendsSuccessChangedResultOnEventBus() throws Exception {
        when(syncJob.resultedInAChange()).thenReturn(true);

        singleJobRequest.processJobResult(syncJob);
        singleJobRequest.finish();

        assertThat(eventBus.lastEventOn(EventQueue.SYNC_RESULT))
                .isEqualTo(SyncJobResult.success(ACTION, true, PLAYLIST_URN));
    }


}