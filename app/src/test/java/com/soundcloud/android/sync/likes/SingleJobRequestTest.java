package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncJob;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class SingleJobRequestTest extends TestCase {

    private SingleJobRequest singleJobRequest;

    @Mock private SyncJob syncJob;

    @Before
    public void setUp() throws Exception {
        singleJobRequest = new SingleJobRequest(syncJob, true);
    }

    @Test
    public void isHighPriorityReturnsValueFromConstructor() throws Exception {
        expect(singleJobRequest.isHighPriority()).toBeTrue();
    }

    @Test
    public void getPendingSyncItemShouldReturnSyncJobWhenNotExecuted() throws Exception {
        expect(singleJobRequest.getPendingJobs()).toContainExactly(syncJob);
    }

    @Test
    public void waitingForSyncItemTrueForSyncJob() throws Exception {
        expect(singleJobRequest.isWaitingForJob(syncJob)).toBeTrue();
    }

    @Test
    public void waitingForSyncItemFalseAfterProcessingSyncJob() throws Exception {
        singleJobRequest.processJobResult(syncJob);
        expect(singleJobRequest.isWaitingForJob(syncJob)).toBeFalse();
    }

    @Test
    public void isSatisfiedIsFalseBeforeProcessingSyncJob() throws Exception {
        expect(singleJobRequest.isSatisfied()).toBeFalse();
    }

    @Test
    public void isSatisfiedIsTrueAfterProcessingSyncJob() throws Exception {
        singleJobRequest.processJobResult(syncJob);
        expect(singleJobRequest.isSatisfied()).toBeTrue();
    }
}