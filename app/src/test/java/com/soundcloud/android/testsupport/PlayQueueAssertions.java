package com.soundcloud.android.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlayableQueueItem;
import org.mockito.ArgumentCaptor;

import java.util.List;

public class PlayQueueAssertions {

    public static void assertPlayNewQueue(PlaySessionController playSessionControllerMock, PlayQueue playQueue, Urn initialTrack, int startIndex, PlaySessionSource playSessionSource) {

        final ArgumentCaptor<PlayQueue> playQueueCaptor = ArgumentCaptor.forClass(PlayQueue.class);
        verify(playSessionControllerMock).playNewQueue(playQueueCaptor.capture(), eq(initialTrack), eq(startIndex), eq(playSessionSource));

        assertPlayQueuesEqual(playQueue, playQueueCaptor.getValue());
    }

    public static void assertPlayQueueSet(PlayQueueManager playQueueManagerMock, PlayQueue playQueue, PlaySessionSource playSessionSource, int startIndex) {
        final ArgumentCaptor<PlayQueue> playQueueCaptor = ArgumentCaptor.forClass(PlayQueue.class);
        verify(playQueueManagerMock).setNewPlayQueue(playQueueCaptor.capture(), eq(playSessionSource), eq(startIndex));

        assertPlayQueuesEqual(playQueue, playQueueCaptor.getValue());
    }

    public static void assertPlayQueuesEqual(PlayQueue playQueue, PlayQueue actualPlayQueue) {
        assertThat(actualPlayQueue.size()).isEqualTo(playQueue.size());
        for (int i = 0; i < actualPlayQueue.size(); i++) {
            assertPlayQueueItemsEqual(actualPlayQueue.getPlayQueueItem(i), playQueue.getPlayQueueItem(i));
        }
    }

    public static void assertPlayQueueItemsEqual(List<? extends PlayQueueItem> playQueueItems1, List<? extends PlayQueueItem> playQueueItems2) {
        assertThat(playQueueItems1.size()).isEqualTo(playQueueItems2.size());
        for (int i = 0; i < playQueueItems1.size(); i++) {
            assertPlayQueueItemsEqual(playQueueItems1.get(i), playQueueItems2.get(i));
        }
    }

    public static void assertPlayQueueItemsEqual(PlayQueueItem playQueueItem1, PlayQueueItem playQueueItem2) {
        assertThat(playQueueItem1.getKind()).isEqualTo(playQueueItem2.getKind());
        assertThat(playQueueItem1.getUrn()).isEqualTo(playQueueItem2.getUrn());
        assertThat(playQueueItem1.getAdData()).isEqualTo(playQueueItem2.getAdData());
        assertThat(playQueueItem1.shouldPersist()).isEqualTo(playQueueItem2.shouldPersist());

        if (playQueueItem1 instanceof PlayableQueueItem) {
            assertThat(((PlayableQueueItem) playQueueItem1).getQueryUrn())
                    .isEqualTo(((PlayableQueueItem) playQueueItem2).getQueryUrn());
            assertThat(((PlayableQueueItem) playQueueItem1).getRelatedEntity())
                    .isEqualTo(((PlayableQueueItem) playQueueItem2).getRelatedEntity());
            assertThat(((PlayableQueueItem) playQueueItem1).getSource())
                    .isEqualTo(((PlayableQueueItem) playQueueItem2).getSource());
            assertThat(((PlayableQueueItem) playQueueItem1).getSourceVersion())
                    .isEqualTo(((PlayableQueueItem) playQueueItem2).getSourceVersion());
            assertThat(((PlayableQueueItem) playQueueItem1).isBlocked())
                    .isEqualTo(((PlayableQueueItem) playQueueItem2).isBlocked());
        }
    }

}
