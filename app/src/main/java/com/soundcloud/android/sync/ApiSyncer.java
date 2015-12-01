package com.soundcloud.android.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.storage.BaseDAO;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;


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

                case TRACK_LOOKUP:
                case USER_LOOKUP:
                    // still reached from search auto suggest
                    result = fetchAndInsertCollection(c, uri);
                    break;

                case TRACK:
                    // used from TrackRepository to fulfill single track requests
                    result = syncSingleTrack(uri);
                    break;
            }
        }

        return result;
    }

    // TODO: this should move out into its own syncer once we have full tracks on api-mobile
    private ApiSyncResult syncSingleTrack(Uri contentUri) throws IOException {
        ApiSyncResult result = new ApiSyncResult(contentUri);
        final long trackId = ContentUris.parseId(contentUri);
        ApiRequest request = ApiRequest.get(ApiEndpoints.LEGACY_TRACK.path(trackId)).forPublicApi().build();

        final PublicApiTrack track;
        try {
            track = apiClient.fetchMappedResponse(request, PublicApiTrack.class);
        } catch (ApiRequestException | ApiMapperException e) {
            // this is because our legacy sync stack only supports IOExceptions
            throw new IOException(e);
        }

        final WriteResult writeResult = storeTracksCommand.call(Collections.singleton(track));
        if (writeResult.success()) {
            log("inserted " + contentUri.toString());
            result.setSyncData(true, System.currentTimeMillis(), 1, ApiSyncResult.CHANGED);
        } else {
            log("failed to create to " + contentUri);
            result.success = false;
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

    /**
     * Fetch Api Resources and create them into the content provider. Plain resource inserts, no extra
     * content values will be inserted
     *
     * @throws IOException
     */
    private ApiSyncResult fetchAndInsertCollection(final Content content, Uri contentUri) throws IOException {
        ApiSyncResult result = new ApiSyncResult(contentUri);
        log("fetchAndInsertCollection(" + contentUri + ")");

        Request request = Request.to(content.remoteUri).add("ids", contentUri.getLastPathSegment());

        if (Content.PLAYLIST_LOOKUP.equals(content)) {
            HttpUtils.addQueryParams(request, "representation", "compact");
        }

        List<PublicApiResource> resources = api.readFullCollection(request, PublicApiResource.ResourceHolder.class);

        new BaseDAO<PublicApiResource>(resolver) {
            @Override
            public Content getContent() {
                return content;
            }
        }.createCollection(resources);
        result.setSyncData(true, System.currentTimeMillis(), resources.size(), ApiSyncResult.CHANGED);
        return result;
    }
}
