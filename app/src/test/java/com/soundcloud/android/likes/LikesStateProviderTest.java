package com.soundcloud.android.likes;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LikesStateProviderTest extends AndroidUnitTest {

    private LikesStateProvider likesStateProvider;
    
    @Mock private LikesStorage likesStorage;
    private final TestSubscriber<LikedStatuses> subscriber = new TestSubscriber<>();
    private final TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        likesStateProvider = new LikesStateProvider(likesStorage, eventBus, Schedulers.immediate());
    }

    @Test
    public void emitsLikeStatusFromStorage() {
        List<Urn> list = asList(Urn.forTrack(1), Urn.forPlaylist(2));
        when(likesStorage.loadLikes()).thenReturn(Observable.just(list));

        likesStateProvider.subscribe();
        likesStateProvider.likedStatuses().subscribe(subscriber);

        subscriber.assertValue(new LikedStatuses(new HashSet<>(list)));
    }

    @Test
    public void emitsLikeStatusFromUpdate() {
        List<Urn> list = asList(Urn.forTrack(1), Urn.forPlaylist(2));
        when(likesStorage.loadLikes()).thenReturn(Observable.just(list));

        Map<Urn, LikesStatusEvent.LikeStatus> urnLikeStatusMap = new HashMap<>();
        urnLikeStatusMap.put(Urn.forTrack(1), LikesStatusEvent.LikeStatus.create(Urn.forTrack(1), true));
        urnLikeStatusMap.put(Urn.forPlaylist(2), LikesStatusEvent.LikeStatus.create(Urn.forPlaylist(2), true));
        urnLikeStatusMap.put(Urn.forTrack(3), LikesStatusEvent.LikeStatus.create(Urn.forTrack(3), false));

        when(likesStorage.loadLikes()).thenReturn(Observable.never());

        likesStateProvider.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.createFromSync(urnLikeStatusMap));
        likesStateProvider.likedStatuses().subscribe(subscriber);

        subscriber.assertValue(new LikedStatuses(new HashSet<>(asList(Urn.forTrack(1), Urn.forPlaylist(2)))));
    }

    @Test
    public void emitsLikeStatusFromStorageAndUpdate() {
        List<Urn> list = Collections.singletonList(Urn.forTrack(1));
        when(likesStorage.loadLikes()).thenReturn(Observable.just(list));

        Map<Urn, LikesStatusEvent.LikeStatus> urnLikeStatusMap = new HashMap<>();
        urnLikeStatusMap.put(Urn.forPlaylist(2), LikesStatusEvent.LikeStatus.create(Urn.forPlaylist(2), true));
        urnLikeStatusMap.put(Urn.forTrack(3), LikesStatusEvent.LikeStatus.create(Urn.forTrack(3), false));

        likesStateProvider.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.createFromSync(urnLikeStatusMap));
        likesStateProvider.likedStatuses().subscribe(subscriber);

        subscriber.assertValue(new LikedStatuses(new HashSet<>(asList(Urn.forTrack(1), Urn.forPlaylist(2)))));
    }
}
