package com.soundcloud.android.cast;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RemotePlayQueueTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final Urn URN2 = Urn.forTrack(456L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(URN, URN2);

    @Test
    public void returnsCurrentTrackPosition() {
        RemotePlayQueue remotePlayQueue = new RemotePlayQueue(PLAY_QUEUE, URN2);
        assertThat(remotePlayQueue.getCurrentPosition()).isEqualTo(1);
    }

}
