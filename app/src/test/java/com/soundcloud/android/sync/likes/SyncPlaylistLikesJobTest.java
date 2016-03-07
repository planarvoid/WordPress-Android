package com.soundcloud.android.sync.likes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.LegacySyncJob;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class SyncPlaylistLikesJobTest extends AndroidUnitTest {

    private SyncPlaylistLikesJob job;

    @Mock private LikesSyncer<ApiPlaylist> likesSyncer;

    @Before
    public void setUp() throws Exception {
        job = new SyncPlaylistLikesJob(InjectionSupport.lazyOf(likesSyncer));
    }

    @Test
    public void equalsAnyJobOfTheSameType() throws Exception {
        assertThat(job.equals(Mockito.mock(SyncPlaylistLikesJob.class))).isTrue();
    }

    @Test
    public void equalsLegacyMyLikesJob() throws Exception {
        LegacySyncJob legacySyncJob = Mockito.mock(LegacySyncJob.class);
        when(legacySyncJob.getContentUri()).thenReturn(Content.ME_LIKES.uri);
        assertThat(job.equals(legacySyncJob)).isTrue();
    }

    @Test
    public void doesNotEqualOtherSyncJob() throws Exception {
        SyncJob syncJob = Mockito.mock(SyncJob.class);
        assertThat(job.equals(syncJob)).isFalse();
    }
}
