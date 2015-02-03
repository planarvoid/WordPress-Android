package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.LegacySyncJob;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.testsupport.InjectionSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(SoundCloudTestRunner.class)
public class SyncPlaylistLikesJobTest {

    private SyncPlaylistLikesJob job;

    @Mock private LikesSyncer<ApiPlaylist> likesSyncer;

    @Before
    public void setUp() throws Exception {
        job = new SyncPlaylistLikesJob(InjectionSupport.lazyOf(likesSyncer));
    }

    @Test
    public void equalsAnyJobOfTheSameType() throws Exception {
        expect(job.equals(Mockito.mock(SyncPlaylistLikesJob.class))).toBeTrue();
    }

    @Test
    public void equalsLegacyMyLikesJob() throws Exception {
        LegacySyncJob legacySyncJob = Mockito.mock(LegacySyncJob.class);
        when(legacySyncJob.getContentUri()).thenReturn(Content.ME_LIKES.uri);
        expect(job.equals(legacySyncJob)).toBeTrue();
    }

    @Test
    public void doesNotEqualOtherSyncJob() throws Exception {
        SyncJob syncJob = Mockito.mock(SyncJob.class);
        expect(job.equals(syncJob)).toBeFalse();
    }
}