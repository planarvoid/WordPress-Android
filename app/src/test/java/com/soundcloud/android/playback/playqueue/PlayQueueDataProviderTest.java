package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class PlayQueueDataProviderTest {

    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueUIItemMapper playQueueUIItemMapper;
    private EventBus eventBus = new TestEventBus();

    private PlayQueueDataProvider playQueueDataProvider;

    @Before
    public void setUp() {
        playQueueDataProvider = new PlayQueueDataProvider(playQueueOperations, playQueueUIItemMapper, eventBus);

    }

    @Test
    public void emitsWhenSubscribedTo() {
        when(playQueueOperations.getTracks()).thenReturn(Observable.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));
        when(playQueueUIItemMapper.call(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestSubscriber<PlayQueueUIItemsUpdate> subscriber = TestSubscriber.create();
        playQueueDataProvider.playQueueUIItemsUpdate().subscribe(subscriber);

        subscriber.assertValueCount(1);
        PlayQueueUIItemsUpdate uiItemsUpdate = subscriber.getOnNextEvents().get(0);

        assertThat(uiItemsUpdate.items()).isEmpty();
        assertThat(uiItemsUpdate.isQueueLoad()).isTrue();
    }

    @Test
    public void emitsUIItemsAfterQueueReorder() {
        when(playQueueOperations.getTracks()).thenReturn(Observable.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));
        when(playQueueUIItemMapper.call(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestSubscriber<PlayQueueUIItemsUpdate> subscriber = TestSubscriber.create();
        playQueueDataProvider.playQueueUIItemsUpdate().subscribe(subscriber);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueReordered(Urn.NOT_SET));

        subscriber.assertValueCount(2);

        PlayQueueUIItemsUpdate firstUpdate = subscriber.getOnNextEvents().get(0);
        assertThat(firstUpdate.items()).isEmpty();
        assertThat(firstUpdate.isQueueLoad()).isTrue();

        PlayQueueUIItemsUpdate secondUpdate = subscriber.getOnNextEvents().get(1);
        assertThat(secondUpdate.items()).isEmpty();
        assertThat(secondUpdate.isQueueReorder()).isTrue();
    }

    @Test
    public void emitsUIItemsAfterNewItemInQueue() {
        when(playQueueOperations.getTracks()).thenReturn(Observable.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.emptyMap()));
        when(playQueueUIItemMapper.call(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestSubscriber<PlayQueueUIItemsUpdate> subscriber = TestSubscriber.create();
        playQueueDataProvider.playQueueUIItemsUpdate().subscribe(subscriber);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueInsert(Urn.NOT_SET));

        subscriber.assertValueCount(2);

        PlayQueueUIItemsUpdate firstUpdate = subscriber.getOnNextEvents().get(0);
        assertThat(firstUpdate.items()).isEmpty();
        assertThat(firstUpdate.isQueueLoad()).isTrue();

        PlayQueueUIItemsUpdate secondUpdate = subscriber.getOnNextEvents().get(1);
        assertThat(secondUpdate.items()).isEmpty();
        assertThat(secondUpdate.isItemAdded()).isTrue();
    }

}
