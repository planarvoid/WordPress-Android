package com.soundcloud.android.sync.likes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.LegacySyncJob;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class SyncTrackLikesJobTest extends AndroidUnitTest {

    private SyncTrackLikesJob job;

    @Mock private LikesSyncer<ApiTrack> likesSyncer;

    @Before
    public void setUp() throws Exception {
        job = new SyncTrackLikesJob(InjectionSupport.lazyOf(likesSyncer));
    }

    @Test
    public void equalsAnyJobOfTheSameType() throws Exception {
        assertThat(job.equals(Mockito.mock(SyncTrackLikesJob.class))).isTrue();
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
