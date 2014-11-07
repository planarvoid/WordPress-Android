package com.soundcloud.android.sync.content;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.stream.SoundStreamWriteStorage;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import javax.inject.Inject;
import java.io.IOException;

public class SoundStreamSyncer implements SyncStrategy {

    @VisibleForTesting
    static final String PREFS_NEXT_URL = "next_url";

    private static final String SHARED_PREFS_NAME = "StreamSync";

    private final SharedPreferences syncPreferences;
    private final ApiClient apiClient;
    private final SoundStreamWriteStorage writeStorage;
    private final TypeToken<ModelCollection<ApiStreamItem>> collectionTypeToken = new TypeToken<ModelCollection<ApiStreamItem>>() { };
    private final Predicate<ApiStreamItem> removePromotedItemsPredicate = new Predicate<ApiStreamItem>() {
        @Override
        public boolean apply(ApiStreamItem input) {
            return !input.isPromotedStreamItem();
        }
    };

    @Inject
    public SoundStreamSyncer(Context appContext, ApiClient apiClient, SoundStreamWriteStorage writeStorage) {
        this(appContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE), apiClient, writeStorage);
    }

    public SoundStreamSyncer(SharedPreferences syncPreferences, ApiClient apiClient, SoundStreamWriteStorage writeStorage) {
        this.syncPreferences = syncPreferences;
        this.apiClient = apiClient;
        this.writeStorage = writeStorage;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws IOException, ApiMapperException {
        Log.d("syncActivities(" + uri + "); action=" + action);

        if (ApiSyncService.ACTION_HARD_REFRESH.equals(action)) {
            return refreshSoundStream();

        } else if (ApiSyncService.ACTION_APPEND.equals(action)) {
            return appendStreamItems();

        }
        return new ApiSyncResult(uri);
    }

    private ApiSyncResult refreshSoundStream() throws IOException, ApiMapperException {

        final ApiRequest.Builder<ModelCollection<ApiStreamItem>> requestBuilder =
                ApiRequest.Builder.<ModelCollection<ApiStreamItem>>get(ApiEndpoints.STREAM.path())
                        .addQueryParam(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.LIST_PAGE_SIZE))
                        .forResource(collectionTypeToken)
                        .forPrivateApi(1);

        ModelCollection<ApiStreamItem> streamItems = apiClient.fetchMappedResponse(requestBuilder.build());
        writeStorage.replaceStreamItems(Iterables.filter(streamItems.getCollection(), removePromotedItemsPredicate));
        setNextPageUrl(streamItems.getNextLink());

        return ApiSyncResult.fromSuccessfulChange(Content.ME_SOUND_STREAM.uri);
    }


    private ApiSyncResult appendStreamItems() throws ApiMapperException {
        if (hasNextPageUrl()){

            final String nextPageUrl = getNextPageUrl();
            Log.d("Building soundstream request from stored next link " + nextPageUrl);

            final ApiRequest.Builder<ModelCollection<ApiStreamItem>> requestBuilder =
                    ApiRequest.Builder.<ModelCollection<ApiStreamItem>>get(nextPageUrl)
                            .forResource(collectionTypeToken)
                            .forPrivateApi(1);

            ModelCollection<ApiStreamItem> streamItems = apiClient.fetchMappedResponse(requestBuilder.build());
            setNextPageUrl(streamItems.getNextLink());

            if (streamItems.getCollection().isEmpty()){
                return ApiSyncResult.fromSuccessWithoutChange(Content.ME_SOUND_STREAM.uri);

            } else {
                writeStorage.insertStreamItems(Iterables.filter(streamItems.getCollection(), removePromotedItemsPredicate));
                return ApiSyncResult.fromSuccessfulChange(Content.ME_SOUND_STREAM.uri);
            }

        } else {

            Log.d("No Next SoundStream page link found. Aborting append");
            return ApiSyncResult.fromSuccessWithoutChange(Content.ME_SOUND_STREAM.uri);
        }
    }

    private String getNextPageUrl() {
        return syncPreferences.getString(PREFS_NEXT_URL, ScTextUtils.EMPTY_STRING);
    }

    private boolean hasNextPageUrl() {
        return syncPreferences.contains(PREFS_NEXT_URL);
    }

    private void setNextPageUrl(Optional<Link> nextLink) {
        if (nextLink.isPresent()) {
            final String href = nextLink.get().getHref();
            Log.d("Writing next soundstream link to preferences : " + href);
            syncPreferences.edit().putString(PREFS_NEXT_URL, href).apply();
        } else {
            Log.d("No next link in soundstream response, clearing any stored link");
            syncPreferences.edit().remove(PREFS_NEXT_URL).apply();
        }
    }

}
