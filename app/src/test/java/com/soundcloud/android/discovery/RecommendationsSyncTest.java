package com.soundcloud.android.discovery;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;

import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class RecommendationsSyncTest {

    private RecommendationsSync syncer;

    @Mock private SyncInitiator syncInitiator;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private DateProvider dateProvider;

    @Before
    public void setUp() {
        syncer = new RecommendationsSync(syncInitiator, sharedPreferences, dateProvider);
    }

    @Test
    public void syncRecommendationsOnlyIfCacheIsExpired() {
        mockRecommendationsSyncDate(TimeUnit.DAYS.toMillis(3), TimeUnit.DAYS.toMillis(1));
        when(syncInitiator.syncRecommendations()).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_RECOMMENDATIONS, true)));

        syncer.syncRecommendations();

        verify(syncInitiator).syncRecommendations();
        verify(sharedPreferences).getLong(anyString(), anyLong());
        verifyNoMoreInteractions(sharedPreferences);
    }

    @Test
    public void doNotSyncRecommendationsIfCacheIsValid() {
        mockRecommendationsSyncDate(TimeUnit.DAYS.toMillis(1), TimeUnit.HOURS.toMillis(2));

        syncer.syncRecommendations();

        verifyZeroInteractions(syncInitiator);
        verify(sharedPreferences).getLong(anyString(), anyLong());
        verifyNoMoreInteractions(sharedPreferences);
    }

    private void mockRecommendationsSyncDate(long currentTime, long elapsedTimeSinceLastSyncMillis) {
        when(dateProvider.getCurrentTime()).thenReturn(currentTime);
        when(sharedPreferences.getLong(anyString(), anyLong())).thenReturn(elapsedTimeSinceLastSyncMillis);
    }
}