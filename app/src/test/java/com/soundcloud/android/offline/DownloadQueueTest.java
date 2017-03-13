package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DownloadQueueTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123);
    private static final Urn TRACK2 = Urn.forTrack(456L);

    private DownloadQueue downloadQueue;

    @Before
    public void setUp() throws Exception {
        downloadQueue = new DownloadQueue();
    }

    @Test
    public void isEmptyReturnsTrue() {
        downloadQueue.set(Collections.emptyList());

        assertThat(downloadQueue.isEmpty()).isTrue();
    }

    @Test
    public void isEmptyReturnsFalse() {
        downloadQueue.set(singletonList(createDownloadRequest(TRACK1)));

        assertThat(downloadQueue.isEmpty()).isFalse();
    }

    @Test
    public void pollReturnsAndRemoveTheFirstRequest() {
        final DownloadRequest request1 = createDownloadRequest(TRACK1);
        final DownloadRequest request2 = createDownloadRequest(TRACK2);
        downloadQueue.set(Arrays.asList(request1, request2));

        assertThat(downloadQueue.poll()).isEqualTo(request1);
        assertThat(downloadQueue.getRequests()).containsExactly(request2);
    }

    private DownloadRequest createDownloadRequest(Urn track) {
        return ModelFixtures.downloadRequestFromLikes(track);
    }

}
