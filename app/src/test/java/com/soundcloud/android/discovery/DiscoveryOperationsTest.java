package com.soundcloud.android.discovery;

import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.DiscoveryOperations;
import com.soundcloud.android.discovery.RecommendationsStorage;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryOperationsTest {

    private DiscoveryOperations operations;

    @Mock private RecommendationsStorage recommendationsStorage;
    @Mock private SyncInitiator syncInitiator;

    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<List<PropertySet>> observer = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new DiscoveryOperations(syncInitiator, recommendationsStorage, scheduler);
        when(syncInitiator.syncRecommendations()).thenReturn(
                Observable.just(SyncResult.success(SyncActions.SYNC_RECOMMENDATIONS, true)));
    }

    @Test
    public void doesSyncAndLoadWhenRecommendationsAreRequested() {
        PublishSubject<SyncResult> syncSubject = PublishSubject.create();
        List<PropertySet> seedTracks = Collections.emptyList();

        when(syncInitiator.syncRecommendations()).thenReturn(syncSubject);
        when(recommendationsStorage.seedTracks()).thenReturn(Observable.just(seedTracks));

        operations.recommendations().subscribe(observer);

        observer.assertNoValues();
        syncSubject.onNext(SyncResult.success("action", true));
        observer.assertReceivedOnNext(Arrays.asList(seedTracks));
    }
}