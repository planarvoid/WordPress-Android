package com.soundcloud.android.sync.entities;

import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.SyncStrategy;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import javax.inject.Inject;
import java.util.Collections;


public class MeSyncer implements SyncStrategy {

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

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        Me me = apiClient.fetchMappedResponse(buildRequest(), new TypeToken<Me>() {});
        storeMe(me);
        publishChangeEvent(me);
        return ApiSyncResult.fromSuccessfulChange(Content.ME.uri);
    }

    protected ApiRequest buildRequest() {
        return ApiRequest.get(ApiEndpoints.ME.path())
                .forPrivateApi()
                .build();
    }

    private void publishChangeEvent(Me me) {
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.forUpdate(me.getUser().toPropertySet()));
    }

    private void storeMe(Me me) {
        storeUsersCommand.call(Collections.singleton(me.getUser()));
    }
}
