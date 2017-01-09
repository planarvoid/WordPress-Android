package com.soundcloud.android.sync.me;

import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.Collections;
import java.util.concurrent.Callable;


public class MeSyncer implements Callable<Boolean> {

    private final EventBus eventBus;
    private final ApiClient apiClient;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    public MeSyncer(ApiClient apiClient,
                    EventBus eventBus,
                    StoreUsersCommand storeUsersCommand) {
        this.apiClient = apiClient;
        this.eventBus = eventBus;
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    public Boolean call() throws Exception {
        Me me = apiClient.fetchMappedResponse(buildRequest(), new TypeToken<Me>() {});
        storeMe(me);
        publishChangeEvent(me);
        return true;
    }

    protected ApiRequest buildRequest() {
        return ApiRequest.get(ApiEndpoints.ME.path())
                         .forPrivateApi()
                         .build();
    }

    private void publishChangeEvent(Me me) {
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, UserItem.from(me.getUser()).toUpdateEvent());
    }

    private void storeMe(Me me) {
        storeUsersCommand.call(Collections.singleton(me.getUser()));
    }
}
