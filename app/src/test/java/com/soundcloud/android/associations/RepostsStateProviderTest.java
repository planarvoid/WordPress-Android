package com.soundcloud.android.associations;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RepostsStatusEvent;
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

public class RepostsStateProviderTest extends AndroidUnitTest {

    private RepostsStateProvider repostsStateProvider;
    
    @Mock private RepostStorage repostStorage;
    private final TestObserver<RepostStatuses> subscriber = new TestObserver<>();
    private final TestEventBusV2 eventBus = new TestEventBusV2();

    @Before
    public void setUp() throws Exception {
        repostsStateProvider = new RepostsStateProvider(repostStorage, eventBus, Schedulers.trampoline());
    }

    @Test
    public void emitsRepostStatusFromStorage() {
        List<Urn> list = asList(Urn.forTrack(1), Urn.forPlaylist(2));
        when(repostStorage.loadReposts()).thenReturn(Single.just(list));

        repostsStateProvider.subscribe();
        repostsStateProvider.repostedStatuses().subscribe(subscriber);

        subscriber.assertValue(RepostStatuses.create(new HashSet<>(list)));
    }

    @Test
    public void emitsRepostStatusFromUpdate() {
        List<Urn> list = asList(Urn.forTrack(1), Urn.forPlaylist(2));
        when(repostStorage.loadReposts()).thenReturn(Single.just(list));

        Map<Urn, RepostsStatusEvent.RepostStatus> urnRepostStatusMap = new HashMap<>();
        urnRepostStatusMap.put(Urn.forTrack(1), RepostsStatusEvent.RepostStatus.createReposted(Urn.forTrack(1)));
        urnRepostStatusMap.put(Urn.forPlaylist(2), RepostsStatusEvent.RepostStatus.createReposted(Urn.forPlaylist(2)));
        urnRepostStatusMap.put(Urn.forTrack(3), RepostsStatusEvent.RepostStatus.createUnposted(Urn.forTrack(3)));

        when(repostStorage.loadReposts()).thenReturn(Single.never());

        repostsStateProvider.subscribe();
        eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.create(urnRepostStatusMap));
        repostsStateProvider.repostedStatuses().subscribe(subscriber);

        subscriber.assertValue(RepostStatuses.create(new HashSet<>(asList(Urn.forTrack(1), Urn.forPlaylist(2)))));
    }

    @Test
    public void emitsRepostStatusFromStorageAndUpdate() {
        List<Urn> list = Collections.singletonList(Urn.forTrack(1));
        when(repostStorage.loadReposts()).thenReturn(Single.just(list));

        Map<Urn, RepostsStatusEvent.RepostStatus> urnRepostStatusMap = new HashMap<>();
        urnRepostStatusMap.put(Urn.forPlaylist(2), RepostsStatusEvent.RepostStatus.createReposted(Urn.forPlaylist(2)));
        urnRepostStatusMap.put(Urn.forTrack(3), RepostsStatusEvent.RepostStatus.createUnposted(Urn.forTrack(3)));

        repostsStateProvider.subscribe();
        eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.create(urnRepostStatusMap));
        repostsStateProvider.repostedStatuses().subscribe(subscriber);

        subscriber.assertValue(RepostStatuses.create(new HashSet<>(asList(Urn.forTrack(1), Urn.forPlaylist(2)))));
    }
}
