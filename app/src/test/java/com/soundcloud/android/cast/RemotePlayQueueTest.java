package com.soundcloud.android.cast;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.testsupport.TestUrns;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RemotePlayQueueTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final Urn URN2 = Urn.forTrack(456L);
    private static final List<Urn> TRACK_LIST = Arrays.asList(URN, URN2);

    @Test
    public void returnsCurrentTrackPosition() {
        RemotePlayQueue remotePlayQueue = RemotePlayQueue.create(TRACK_LIST, URN2);
        assertThat(remotePlayQueue.getCurrentPosition()).isEqualTo(1);
    }

    @Test
    public void hasSameTrackListTrueForMatchingUrns() {
        RemotePlayQueue remotePlayQueue = RemotePlayQueue.create(TRACK_LIST, URN);
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(TRACK_LIST, PlaySessionSource.EMPTY, Collections.emptyMap());

        assertThat(remotePlayQueue.hasSameTracks(playQueue)).isTrue();
    }

    @Test
    public void hasSameTrackListFalseForDifferentOrder() {
        RemotePlayQueue remotePlayQueue = RemotePlayQueue.create(TestUrns.createTrackUrns(1L, 2L, 3L), URN);
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(3L, 2L, 1L), PlaySessionSource.EMPTY, Collections.emptyMap());

        assertThat(remotePlayQueue.hasSameTracks(playQueue)).isFalse();
    }
}