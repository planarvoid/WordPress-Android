package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class PlayQueueItemTest {

    @Test
    public void isNotEqualWithDifferentInstance() {
        assertThat(new TestPlayQueueItem()).isNotEqualTo(new TestPlayQueueItem());
    }

    @Test
    public void isEqualWithSameInstance() {
        final TestPlayQueueItem playQueueItem = new TestPlayQueueItem();
        assertThat(playQueueItem).isEqualTo(playQueueItem);
    }

    private static class TestPlayQueueItem extends PlayQueueItem {

        @Override
        public Urn getUrn() {
            return Urn.forTrack(123);
        }

        @Override
        public boolean shouldPersist() {
            return true;
        }

        @Override
        public Kind getKind() {
            return Kind.TRACK;
        }
    }
}
