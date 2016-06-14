package com.soundcloud.android.discovery;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.LegacySyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RecommendedTracksSyncInitiatorTest extends AndroidUnitTest {

    private RecommendedTracksSyncInitiator recommendedTracksSyncInitiator;

    @Mock private LegacySyncInitiator syncInitiator;
    private TestDateProvider dateProvider;


    @Before
    public void setUp() {
        dateProvider = new TestDateProvider();
        SharedPreferences sharedPreferences = AndroidUnitTest.sharedPreferences("Discovery Shared Prefs", Context.MODE_PRIVATE);
        recommendedTracksSyncInitiator = new RecommendedTracksSyncInitiator(syncInitiator, sharedPreferences, dateProvider);
    }

    @Test
    public void syncRecommendationsOnlyIfCacheIsExpired() {
        dateProvider.setTime((long) 3, TimeUnit.DAYS);
        when(syncInitiator.syncRecommendedTracks()).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_RECOMMENDED_TRACKS, true)));

        assertResult(recommendedTracksSyncInitiator.sync(), Collections.singletonList(true));

        verify(syncInitiator).syncRecommendedTracks();
    }

    @Test
    public void doNotSyncRecommendationsIfCacheIsValid() {
        dateProvider.setTime((long) 1, TimeUnit.DAYS);

        assertResult(recommendedTracksSyncInitiator.sync(), Collections.singletonList(false));

        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void gracefullyRecoversIfSyncFails() {
        dateProvider.setTime((long) 1, TimeUnit.DAYS);

        when(syncInitiator.syncRecommendedTracks()).thenReturn(Observable.<SyncResult>error(new RuntimeException("expected")));

        assertResult(recommendedTracksSyncInitiator.sync(), Collections.singletonList(false));
    }

    private <T> void assertResult(Observable<T> observable, List<T> expected) {
        TestSubscriber<T> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.assertReceivedOnNext(expected);
    }

}
