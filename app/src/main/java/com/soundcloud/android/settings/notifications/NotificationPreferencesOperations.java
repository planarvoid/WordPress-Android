package com.soundcloud.android.settings.notifications;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

class NotificationPreferencesOperations {

    private static final long STALE_PREFERENCES = TimeUnit.MINUTES.toMillis(15);

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final NotificationPreferencesStorage storage;
    private final NetworkConnectionHelper connectionHelper;

    @Inject
    NotificationPreferencesOperations(ApiClientRx apiClientRx,
                                      @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                                      NotificationPreferencesStorage storage,
                                      NetworkConnectionHelper connectionHelper) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.storage = storage;
        this.connectionHelper = connectionHelper;
    }

    Observable<NotificationPreferences> refresh() {
        return storage.isPendingSync()
               ? sync().flatMap(fetchOnSuccess())
               : fetch();
    }

    Observable<ApiResponse> sync() {
        storage.setPendingSync(true);
        return apiClientRx
                .response(buildSyncRequest())
                .doOnNext(updatePendingSync())
                .subscribeOn(scheduler);
    }

    boolean needsSyncOrRefresh() {
        boolean isStale = storage.getLastUpdateAgo() >= STALE_PREFERENCES;
        return connectionHelper.isNetworkConnected() && (storage.isPendingSync() || isStale);
    }

    private Observable<NotificationPreferences> fetch() {
        return apiClientRx
                .mappedResponse(buildFetchRequest(), NotificationPreferences.class)
                .doOnNext(updateStorage())
                .subscribeOn(scheduler);
    }

    boolean restore(String key) {
        return storage.getBackup(key);
    }

    void backup(String key) {
        storage.storeBackup(key);
    }

    private Func1<ApiResponse, Observable<NotificationPreferences>> fetchOnSuccess() {
        return apiResponse -> {
            if (apiResponse.isSuccess()) {
                return fetch();
            } else {
                return Observable.just(storage.buildNotificationPreferences());
            }
        };
    }

    private Action1<NotificationPreferences> updateStorage() {
        return notificationPreferences -> {
            storage.update(notificationPreferences);
            storage.setUpdated();
        };
    }

    private Action1<ApiResponse> updatePendingSync() {
        return apiResponse -> {
            if (apiResponse.isSuccess()) {
                storage.setPendingSync(false);
            }
        };
    }

    private ApiRequest buildFetchRequest() {
        return ApiRequest
                .get(ApiEndpoints.NOTIFICATION_PREFERENCES.path())
                .forPrivateApi()
                .build();
    }

    private ApiRequest buildSyncRequest() {
        return ApiRequest
                .put(ApiEndpoints.NOTIFICATION_PREFERENCES.path())
                .withContent(storage.buildNotificationPreferences())
                .forPrivateApi()
                .build();
    }
}
