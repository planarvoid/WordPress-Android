package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.rx.eventbus.EventBusV2;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class PlayQueueDataProviderTest {

    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueUIItemMapper playQueueUIItemMapper;
    private EventBusV2 eventBus = new TestEventBusV2();

    private PlayQueueDataProvider playQueueDataProvider;

    @Before
    public void setUp() {
        playQueueDataProvider = new PlayQueueDataProvider(playQueueOperations, playQueueUIItemMapper, eventBus);
    }

    @Test
    public void emitsWhenSubscribedTo() {
        when(playQueueOperations.getTracks()).thenReturn(Single.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Single.just(Collections.emptyMap()));
        when(playQueueUIItemMapper.apply(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestObserver<PlayQueueUIItemsUpdate> testObserver = playQueueDataProvider.playQueueUIItemsUpdate().test();

        testObserver.assertValueCount(1);
        PlayQueueUIItemsUpdate uiItemsUpdate = testObserver.values().get(0);

        assertThat(uiItemsUpdate.items()).isEmpty();
        assertThat(uiItemsUpdate.isQueueLoad()).isTrue();
    }

    @Test
    public void emitsUIItemsAfterQueueReorder() {
        when(playQueueOperations.getTracks()).thenReturn(Single.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Single.just(Collections.emptyMap()));
        when(playQueueUIItemMapper.apply(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestObserver<PlayQueueUIItemsUpdate> testObserver = playQueueDataProvider.playQueueUIItemsUpdate().test();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueReordered(Urn.NOT_SET));

        testObserver.assertValueCount(2);

        PlayQueueUIItemsUpdate firstUpdate = testObserver.values().get(0);
        assertThat(firstUpdate.items()).isEmpty();
        assertThat(firstUpdate.isQueueLoad()).isTrue();

        PlayQueueUIItemsUpdate secondUpdate = testObserver.values().get(1);
        assertThat(secondUpdate.items()).isEmpty();
        assertThat(secondUpdate.isQueueReorder()).isTrue();
    }

    @Test
    public void emitsUIItemsAfterNewItemInQueue() {
        when(playQueueOperations.getTracks()).thenReturn(Single.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Single.just(Collections.emptyMap()));
        when(playQueueUIItemMapper.apply(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestObserver<PlayQueueUIItemsUpdate> testObserver = playQueueDataProvider.playQueueUIItemsUpdate().test();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueInsert(Urn.NOT_SET));

        testObserver.assertValueCount(2);

        PlayQueueUIItemsUpdate firstUpdate = testObserver.values().get(0);
        assertThat(firstUpdate.items()).isEmpty();
        assertThat(firstUpdate.isQueueLoad()).isTrue();

        PlayQueueUIItemsUpdate secondUpdate = testObserver.values().get(1);
        assertThat(secondUpdate.items()).isEmpty();
        assertThat(secondUpdate.isItemAdded()).isTrue();
    }

}
