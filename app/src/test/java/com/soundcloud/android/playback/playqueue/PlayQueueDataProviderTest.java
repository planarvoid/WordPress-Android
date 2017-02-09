package com.soundcloud.android.playback.playqueue;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.List;

public class PlayQueueDataProviderTest {

    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueUIItemMapper playQueueUIItemMapper;
    private EventBus eventBus = new TestEventBus();

    private PlayQueueDataProvider playQueueDataProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        playQueueDataProvider = new PlayQueueDataProvider(playQueueOperations, playQueueUIItemMapper, eventBus);
    }

    @Test
    public void emitsWhenSubscribedTo() {
        when(playQueueOperations.getTracks()).thenReturn(Observable.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.EMPTY_MAP));
        when(playQueueUIItemMapper.call(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestSubscriber<List<PlayQueueUIItem>> subscriber = TestSubscriber.create();
        playQueueDataProvider.getPlayQueueUIItems().subscribe(subscriber);

        subscriber.assertValueCount(1);
        subscriber.assertValues(Lists.emptyList());
    }

    @Test
    public void emitsUIItemsAfterQueueReorder() {
        when(playQueueOperations.getTracks()).thenReturn(Observable.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.EMPTY_MAP));
        when(playQueueUIItemMapper.call(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestSubscriber<List<PlayQueueUIItem>> subscriber = TestSubscriber.create();
        playQueueDataProvider.getPlayQueueUIItems().subscribe(subscriber);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueReordered(Urn.NOT_SET));


        subscriber.assertValueCount(2);
        subscriber.assertValues(Lists.emptyList(), Lists.emptyList());
    }

    @Test
    public void emitsUIItemsAfterNewItemInQueue() {
        when(playQueueOperations.getTracks()).thenReturn(Observable.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.EMPTY_MAP));
        when(playQueueUIItemMapper.call(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestSubscriber<List<PlayQueueUIItem>> subscriber = TestSubscriber.create();
        playQueueDataProvider.getPlayQueueUIItems().subscribe(subscriber);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueInsert(Urn.NOT_SET));


        subscriber.assertValueCount(2);
        subscriber.assertValues(Lists.emptyList(), Lists.emptyList());
    }

    @Test
    public void emitsUIItemsAfterSecondCurrentEvent() {
        when(playQueueOperations.getTracks()).thenReturn(Observable.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.EMPTY_MAP));
        when(playQueueUIItemMapper.call(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestSubscriber<List<PlayQueueUIItem>> subscriber = TestSubscriber.create();
        playQueueDataProvider.getPlayQueueUIItems().subscribe(subscriber);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(PlayQueueItem.EMPTY, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(PlayQueueItem.EMPTY, Urn.NOT_SET, 0));


        subscriber.assertValueCount(2);
        subscriber.assertValues(Lists.emptyList(), Lists.emptyList());
    }

    @Test
    public void ignoresFirstCurrentEvent() {
        when(playQueueOperations.getTracks()).thenReturn(Observable.just(Lists.emptyList()));
        when(playQueueOperations.getContextTitles()).thenReturn(Observable.just(Collections.EMPTY_MAP));
        when(playQueueUIItemMapper.call(anyList(), anyMap())).thenReturn(Lists.emptyList());

        TestSubscriber<List<PlayQueueUIItem>> subscriber = TestSubscriber.create();
        playQueueDataProvider.getPlayQueueUIItems().subscribe(subscriber);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(PlayQueueItem.EMPTY, Urn.NOT_SET, 0));

        subscriber.assertValueCount(1);
        subscriber.assertValues(Lists.emptyList());
    }


}
