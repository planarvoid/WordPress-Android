package com.soundcloud.android.likes;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LikesStateProviderTest extends AndroidUnitTest {

    private LikesStateProvider likesStateProvider;
    
    @Mock private LikesStorage likesStorage;
    private final TestEventBusV2 eventBus = new TestEventBusV2();

    @Before
    public void setUp() throws Exception {
        likesStateProvider = new LikesStateProvider(likesStorage, eventBus, Schedulers.trampoline());
    }

    @Test
    public void emitsLikeStatusFromStorage() {
        List<Urn> list = asList(Urn.forTrack(1), Urn.forPlaylist(2));
        when(likesStorage.loadLikes()).thenReturn(Single.just(list));

        likesStateProvider.subscribe();
        final TestObserver<LikedStatuses> subscriber = likesStateProvider.likedStatuses().test();

        subscriber.assertValue(LikedStatuses.create(new HashSet<>(list)));
    }

    @Test
    public void emitsLikeStatusFromUpdate() {
        List<Urn> list = asList(Urn.forTrack(1), Urn.forPlaylist(2));
        when(likesStorage.loadLikes()).thenReturn(Single.just(list));

        Map<Urn, LikesStatusEvent.LikeStatus> urnLikeStatusMap = new HashMap<>();
        urnLikeStatusMap.put(Urn.forTrack(1), LikesStatusEvent.LikeStatus.create(Urn.forTrack(1), true));
        urnLikeStatusMap.put(Urn.forPlaylist(2), LikesStatusEvent.LikeStatus.create(Urn.forPlaylist(2), true));
        urnLikeStatusMap.put(Urn.forTrack(3), LikesStatusEvent.LikeStatus.create(Urn.forTrack(3), false));

        when(likesStorage.loadLikes()).thenReturn(Single.never());

        likesStateProvider.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.createFromSync(urnLikeStatusMap));
        final TestObserver<LikedStatuses> subscriber = likesStateProvider.likedStatuses().test();

        subscriber.assertValue(LikedStatuses.create(new HashSet<>(asList(Urn.forTrack(1), Urn.forPlaylist(2)))));
    }

    @Test
    public void emitsLikeStatusFromStorageAndUpdate() {
        List<Urn> list = Collections.singletonList(Urn.forTrack(1));
        when(likesStorage.loadLikes()).thenReturn(Single.just(list));

        Map<Urn, LikesStatusEvent.LikeStatus> urnLikeStatusMap = new HashMap<>();
        urnLikeStatusMap.put(Urn.forPlaylist(2), LikesStatusEvent.LikeStatus.create(Urn.forPlaylist(2), true));
        urnLikeStatusMap.put(Urn.forTrack(3), LikesStatusEvent.LikeStatus.create(Urn.forTrack(3), false));

        likesStateProvider.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.createFromSync(urnLikeStatusMap));
        final TestObserver<LikedStatuses> subscriber = likesStateProvider.likedStatuses().test();

        subscriber.assertValue(LikedStatuses.create(new HashSet<>(asList(Urn.forTrack(1), Urn.forPlaylist(2)))));
    }
}
