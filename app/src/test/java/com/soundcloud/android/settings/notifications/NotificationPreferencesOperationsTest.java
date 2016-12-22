package com.soundcloud.android.settings.notifications;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.matchers.ApiRequestTo;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

public class NotificationPreferencesOperationsTest extends AndroidUnitTest {

    public static final Observable<ApiResponse> SUCCESS_RESPONSE = Observable.just(TestApiResponses.ok());
    public static final Observable<ApiResponse> ERROR_RESPONSE = Observable.just(TestApiResponses.networkError());

    @Mock private ApiClientRx apiClientRx;
    @Mock private NotificationPreferencesStorage storage;
    @Mock private NetworkConnectionHelper connectionHelper;

    NotificationPreferencesOperations operations;

    private NotificationPreferences content;

    @Before
    public void setUp() {
        content = buildContent();
        operations = new NotificationPreferencesOperations(apiClientRx,
                                                           Schedulers.immediate(), storage, connectionHelper);
    }

    @Test
    public void shouldClearPendingSyncOnSuccessSync() {
        when(apiClientRx.response(putRequest())).thenReturn(SUCCESS_RESPONSE);

        operations.sync().subscribe();

        InOrder inOrder = inOrder(storage);
        inOrder.verify(storage).setPendingSync(true);
        inOrder.verify(storage).setPendingSync(false);
    }

    @Test
    public void shouldKeepPendingSyncOnErrorSync() {
        when(apiClientRx.response(putRequest())).thenReturn(ERROR_RESPONSE);

        operations.sync().subscribe();

        verify(storage).setPendingSync(true);
        verify(storage, never()).setPendingSync(false);
    }

    @Test
    public void shouldSetContentOnSync() {
        when(storage.buildNotificationPreferences()).thenReturn(content);
        when(apiClientRx.response(putRequestWithContent(content))).thenReturn(SUCCESS_RESPONSE);

        operations.sync().subscribe();

        verify(storage).setPendingSync(false);
    }

    @Test
    public void shouldDelegateRestoreToStorage() {
        operations.restore("test");

        verify(storage).getBackup("test");
    }

    @Test
    public void shouldDelegateBackupToStorage() {
        operations.backup("test");

        verify(storage).storeBackup("test");
    }

    @Test
    public void shouldSyncWhenIsPendingSyncOnRefresh() {
        when(apiClientRx.response(putRequest())).thenReturn(SUCCESS_RESPONSE);
        when(storage.isPendingSync()).thenReturn(true);
        when(apiClientRx.mappedResponse(getRequest(), eq(NotificationPreferences.class)))
                .thenReturn(Observable.just(content));

        operations.refresh().subscribe();

        verify(storage).setPendingSync(false);
    }

    @Test
    public void shouldNotSyncWhenIsNotPendingSyncOnRefresh() {
        when(storage.isPendingSync()).thenReturn(false);
        when(apiClientRx.mappedResponse(getRequest(), eq(NotificationPreferences.class)))
                .thenReturn(Observable.just(content));

        operations.refresh().subscribe();

        verify(apiClientRx, never()).response(putRequest());
    }

    @Test
    public void shouldSetUpdatedOnSuccessfulRefresh() {
        when(apiClientRx.mappedResponse(getRequest(), eq(NotificationPreferences.class)))
                .thenReturn(Observable.just(content));

        operations.refresh().subscribe();

        verify(storage).setUpdated();
    }

    @Test
    public void shouldNotSetUpdatedOnFailedRefresh() {
        when(apiClientRx.mappedResponse(getRequest(), eq(NotificationPreferences.class)))
                .thenReturn(Observable.<NotificationPreferences>error(new Exception()));

        operations.refresh().subscribe(new TestSubscriber<>());

        verify(storage, never()).setUpdated();
    }

    @Test
    public void shouldNeedSyncOrRefreshWhenIsPendingSync() {
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        when(storage.isPendingSync()).thenReturn(true);
        when(storage.getLastUpdateAgo()).thenReturn(TimeUnit.MINUTES.toMillis(3));

        boolean result = operations.needsSyncOrRefresh();

        assertThat(result).isTrue();
    }

    @Test
    public void shouldNeedSyncOrRefreshWhenIsStale() {
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        when(storage.isPendingSync()).thenReturn(false);
        when(storage.getLastUpdateAgo()).thenReturn(TimeUnit.MINUTES.toMillis(15));

        boolean result = operations.needsSyncOrRefresh();

        assertThat(result).isTrue();
    }

    @Test
    public void shouldNotNeedSyncOrRefreshWhenNotConnected() {
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(storage.isPendingSync()).thenReturn(true);
        when(storage.getLastUpdateAgo()).thenReturn(TimeUnit.MINUTES.toMillis(15));

        boolean result = operations.needsSyncOrRefresh();

        assertThat(result).isFalse();
    }

    @Test
    public void shouldNotNeedSyncOrRefresh() {
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        when(storage.isPendingSync()).thenReturn(false);
        when(storage.getLastUpdateAgo()).thenReturn(TimeUnit.MINUTES.toMillis(3));

        boolean result = operations.needsSyncOrRefresh();

        assertThat(result).isFalse();
    }

    private ApiRequest getRequest() {
        return argThat(isApiRequestTo("GET", "/notification_preferences"));
    }

    private ApiRequest putRequest() {
        return argThat(putRequestTo());
    }

    private NotificationPreferences buildContent() {
        NotificationPreferences content = new NotificationPreferences();
        content.add("test", new NotificationPreference(true, false));
        return content;
    }

    private ApiRequest putRequestWithContent(NotificationPreferences content) {
        return argThat(putRequestTo().withContent(content));
    }

    private ApiRequestTo putRequestTo() {
        return isApiRequestTo("PUT", "/notification_preferences");
    }

}
