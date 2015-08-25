package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PolicyUpdateControllerTest extends AndroidUnitTest {
    private PolicyUpdateController controller;

    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private DateProvider dateProvider;
    @Mock private GoBackOnlineDialogPresenter goOnlinePresenter;
    @Mock private Context context;

    private long yesterday;
    private long now;
    private long tomorrow;
    private long online27DaysAgo;
    private long online30DaysAgo;
    private long online33DaysAgo;

    @Before
    public void setUp() throws Exception {
        now = System.currentTimeMillis();
        yesterday = now - TimeUnit.DAYS.toMillis(1);
        tomorrow = now + TimeUnit.DAYS.toMillis(1);
        controller = new PolicyUpdateController(
                context,
                featureOperations,
                offlineContentOperations,
                offlineSettingsStorage,
                dateProvider,
                goOnlinePresenter
        );
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.just(yesterday));
        when(dateProvider.getCurrentTime()).thenReturn(now);
        when(offlineContentOperations.clearOfflineContent()).thenReturn(Observable.<List<Urn>>empty());

        online27DaysAgo = now - TimeUnit.DAYS.toMillis(27L);
        online30DaysAgo = now - TimeUnit.DAYS.toMillis(30L);
        online33DaysAgo = now - TimeUnit.DAYS.toMillis(33L);
    }

    @Test
    public void checksPoliciesOnlyOnceADay() {
        when(offlineSettingsStorage.getPolicyUpdateCheckTime()).thenReturn(now);
        controller.onResume(null);

        verify(offlineContentOperations, never()).tryToUpdateAndLoadLastPoliciesUpdateTime();
    }

    @Test
    public void checksPoliciesEveryDay() {
        when(dateProvider.getCurrentTime()).thenReturn(yesterday);
        controller.onResume(null);
        when(dateProvider.getCurrentTime()).thenReturn(tomorrow);
        controller.onResume(null);

        verify(offlineContentOperations, times(2)).tryToUpdateAndLoadLastPoliciesUpdateTime();
    }

    @Test
    public void updatesTheNextTimeIfTheFirstTimeFailed() {
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.<Long>error(new RuntimeException("Test exception")));

        controller.onResume(null);
        controller.onResume(null);

        verify(offlineContentOperations, times(2)).tryToUpdateAndLoadLastPoliciesUpdateTime();
    }

    @Test
    public void showsGoBackOnlineDialogWhenLastUpdate27DaysAgo() {
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.just(online27DaysAgo));

        controller.onResume(null);

        verify(goOnlinePresenter).show(null, online27DaysAgo);
    }

    @Test
    public void doesNotDisplayTheDialogWhenOffContentDisabled() {
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.just(online27DaysAgo));
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        controller.onResume(null);

        verify(goOnlinePresenter, never()).show(any(Activity.class), anyLong());
    }

    @Test
    public void doesNotDeleteOfflineContentWhenLastUpdate27DaysAgo() {
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.just(online27DaysAgo));

        controller.onResume(null);

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void deletesOfflineContentWhenLastUpdate30DaysAgo() {
        final PublishSubject<List<Urn>> clearOfflineContentSubject = PublishSubject.create();

        when(offlineContentOperations.clearOfflineContent()).thenReturn(clearOfflineContentSubject);
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.just(online30DaysAgo));

        controller.onResume(null);

        assertThat(clearOfflineContentSubject.hasObservers()).isTrue();
    }

    @Test
    public void deletesOfflineContentWhenLastUpdate33DaysAgo() {
        final PublishSubject<List<Urn>> clearOfflineContentSubject = PublishSubject.create();
        when(offlineContentOperations.clearOfflineContent()).thenReturn(clearOfflineContentSubject);
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.just(online33DaysAgo));

        controller.onResume(null);

        assertThat(clearOfflineContentSubject.hasObservers()).isTrue();
    }
}