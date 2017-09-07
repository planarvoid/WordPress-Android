package com.soundcloud.android.sync.me;

import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.accounts.MeStorage;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.users.User;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.rx.eventbus.EventBusV2;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class MeSyncer implements Callable<Boolean> {

    private final EventBusV2 eventBus;
    private final ApiClient apiClient;
    private final MeStorage meStorage;

    @Inject
    public MeSyncer(ApiClient apiClient,
                    EventBusV2 eventBus,
                    MeStorage meStorage) {
        this.apiClient = apiClient;
        this.eventBus = eventBus;
        this.meStorage = meStorage;
    }

    @Override
    public Boolean call() throws Exception {
        Me me = apiClient.fetchMappedResponse(buildRequest(), new TypeToken<Me>() {});
        meStorage.store(me);
        publishChangeEvent(me);
        return true;
    }

    private ApiRequest buildRequest() {
        return ApiRequest.get(ApiEndpoints.ME.path())
                         .forPrivateApi()
                         .build();
    }

    private void publishChangeEvent(Me me) {
        eventBus.publish(EventQueue.USER_CHANGED, UserChangedEvent.forUpdate(User.fromApiUser(me.getUser())));
    }

}
