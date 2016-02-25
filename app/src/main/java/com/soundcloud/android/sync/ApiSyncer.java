package com.soundcloud.android.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.io.IOException;


/**
 * Performs the actual sync with the API. Used by {@link LegacySyncJob}.
 * <p/>
 * As a client, do not use this class directly, but use {@link com.soundcloud.android.sync.SyncInitiator} instead.
 * <p/>
 */
public class ApiSyncer extends LegacySyncStrategy {

    @Inject LegacyUserStorage userStorage;
    @Inject EventBus eventBus;
    @Inject ApiClient apiClient;
    @Inject StoreTracksCommand storeTracksCommand;

    public ApiSyncer(Context context, ContentResolver resolver) {
        super(context, resolver);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ApiSyncer(Context context, ContentResolver resolver, EventBus eventBus, ApiClient apiClient,
              AccountOperations accountOperations, StoreTracksCommand storeTracksCommand) {
        super(context, resolver, accountOperations);
        userStorage = new LegacyUserStorage();
        this.eventBus = eventBus;
        this.apiClient = apiClient;
        this.storeTracksCommand = storeTracksCommand;
    }

    @NotNull
    public ApiSyncResult syncContent(Uri uri, String action) throws IOException {
        final long userId = accountOperations.getLoggedInUserId();
        Content c = Content.match(uri);
        ApiSyncResult result = new ApiSyncResult(uri);

        if (userId <= 0) {
            Log.w(TAG, "Invalid user id, skipping sync ");
        } else if (c.remoteUri != null) {
            switch (c) {
                case ME:
                    // still reached from system sync
                    result = syncMe(c);
                    if (result.success) {
                        resolver.notifyChange(Content.ME.uri, null);
                        PublicApiUser loggedInUser = accountOperations.getLoggedInUser();
                        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(loggedInUser));
                    }
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putLong(Consts.PrefKeys.LAST_USER_SYNC, System.currentTimeMillis())
                            .apply();

                    break;
            }
        }

        return result;
    }

    private ApiSyncResult syncMe(Content c) {
        ApiSyncResult result = new ApiSyncResult(c.uri);
        PublicApiUser user;
        ApiRequest request = ApiRequest.get(ApiEndpoints.CURRENT_USER.path())
                .forPublicApi()
                .build();
        try {
            user = apiClient.fetchMappedResponse(request, PublicApiUser.class);
            user.setUpdated();
            SoundCloudApplication.sModelManager.cache(user, PublicApiResource.CacheUpdateMode.FULL);
        } catch (IOException | ApiRequestException | ApiMapperException e) {
            e.printStackTrace();
            user = null;
        }
        result.synced_at = System.currentTimeMillis();
        if (user != null) {
            userStorage.createOrUpdate(user);
            result.change = ApiSyncResult.CHANGED;
            result.success = true;
        }
        return result;
    }

}
