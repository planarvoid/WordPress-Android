package com.soundcloud.android.settings.notifications;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import javax.inject.Inject;
import javax.inject.Named;

class NotificationPreferencesOperations {

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final NotificationPreferencesStorage storage;

    @Inject
    NotificationPreferencesOperations(ApiClientRx apiClientRx,
                                             @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                                             NotificationPreferencesStorage storage) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.storage = storage;
    }

    Observable<ApiResponse> sync() {
        storage.setPendingSync(true);
        return apiClientRx
                .response(buildSyncRequest())
                .doOnNext(updateSyncFromResponse())
                .subscribeOn(scheduler);
    }

    private Action1<ApiResponse> updateSyncFromResponse() {
        return new Action1<ApiResponse>() {
            @Override
            public void call(ApiResponse apiResponse) {
                if (apiResponse.isSuccess()) {
                    storage.setPendingSync(false);
                }
            }
        };
    }

    boolean restore(String key) {
        return storage.getBackup(key);
    }

    void backup(String key) {
        storage.storeBackup(key);
    }

    private ApiRequest buildSyncRequest() {
        return ApiRequest
                .put(ApiEndpoints.NOTIFICATION_PREFERENCES.path())
                .withContent(storage.buildNotificationPreferences())
                .forPrivateApi(1)
                .build();
    }
}
