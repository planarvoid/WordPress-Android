package com.soundcloud.android.settings.notifications;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.matchers.ApiRequestTo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

public class NotificationPreferencesOperationsTest extends AndroidUnitTest {

    public static final Observable<ApiResponse> SUCCESS_RESPONSE = Observable.just(TestApiResponses.ok());
    public static final Observable<ApiResponse> ERROR_RESPONSE = Observable.just(TestApiResponses.networkError());

    @Mock private ApiClientRx apiClientRx;
    @Mock private NotificationPreferencesStorage storage;

    NotificationPreferencesOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new NotificationPreferencesOperations(apiClientRx, Schedulers.immediate(), storage);
    }

    @Test
    public void shouldClearPendingSyncOnSuccessSync() throws Exception {
        when(apiClientRx.response(putRequest())).thenReturn(SUCCESS_RESPONSE);

        operations.sync().subscribe();

        InOrder inOrder = inOrder(storage);
        inOrder.verify(storage).setPendingSync(true);
        inOrder.verify(storage).setPendingSync(false);
    }

    @Test
    public void shouldKeepPendingSyncOnErrorSync() throws Exception {
        when(apiClientRx.response(putRequest())).thenReturn(ERROR_RESPONSE);

        operations.sync().subscribe();

        verify(storage).setPendingSync(true);
        verify(storage, never()).setPendingSync(false);
    }

    @Test
    public void shouldSetContentOnSync() throws Exception {
        NotificationPreferences content = buildContent();
        when(storage.buildNotificationPreferences()).thenReturn(content);
        when(apiClientRx.response(putRequestWithContent(content))).thenReturn(SUCCESS_RESPONSE);

        operations.sync().subscribe();

        verify(storage).setPendingSync(false);
    }

    @Test
    public void shouldDelegateRestoreToStorage() throws Exception {
        operations.restore("test");

        verify(storage).getBackup("test");
    }

    @Test
    public void shouldDelegateBackupToStorage() throws Exception {
        operations.backup("test");

        verify(storage).storeBackup("test");
    }

    private ApiRequest putRequest() {
        return argThat(apiRequestTo());
    }

    private NotificationPreferences buildContent() {
        NotificationPreferences content = new NotificationPreferences();
        content.add("test", new NotificationPreference(true, false));
        return content;
    }

    private ApiRequest putRequestWithContent(NotificationPreferences content) {
        return argThat(apiRequestTo().withContent(content));
    }

    private ApiRequestTo apiRequestTo() {
        return isApiRequestTo("PUT", "/notification_preferences");
    }

}
