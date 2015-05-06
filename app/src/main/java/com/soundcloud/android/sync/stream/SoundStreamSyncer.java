package com.soundcloud.android.sync.stream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import javax.inject.Inject;
import java.util.Map;

public class SoundStreamSyncer implements SyncStrategy {

    @VisibleForTesting
    static final String FUTURE_LINK_REL = "future";
    static final String PREFS_NEXT_URL = "next_url";
    static final String PREFS_FUTURE_URL = "future_url";

    private static final String SHARED_PREFS_NAME = "StreamSync";

    private final SharedPreferences syncPreferences;
    private final ApiClient apiClient;
    private final StoreSoundStreamCommand storeSoundStreamCommand;
    private final ReplaceSoundStreamCommand replaceSoundStreamCommand;
    private final FeatureFlags flags;
    private final TypeToken<ModelCollection<ApiStreamItem>> collectionTypeToken = new TypeToken<ModelCollection<ApiStreamItem>>() {};

    private final Predicate<ApiStreamItem> removePromotedItemsPredicate = new Predicate<ApiStreamItem>() {
        @Override
        public boolean apply(ApiStreamItem input) {
            return !input.isPromotedStreamItem();
        }
    };

    @Inject
    public SoundStreamSyncer(Context appContext, ApiClient apiClient, StoreSoundStreamCommand storeSoundStreamCommand, ReplaceSoundStreamCommand replaceSoundStreamCommand, FeatureFlags flags) {
        this.syncPreferences = appContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        this.apiClient = apiClient;
        this.storeSoundStreamCommand = storeSoundStreamCommand;
        this.replaceSoundStreamCommand = replaceSoundStreamCommand;
        this.flags = flags;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws Exception  {
        Log.d(this, "syncActivities(" + uri + "); action=" + action);

        if (ApiSyncService.ACTION_APPEND.equals(action)) {
            return appendStreamItems();

        } else if (ApiSyncService.ACTION_HARD_REFRESH.equals(action) || missingFuturePageUrl()) {
            return refreshSoundStream();

        } else {
            return prependActivitiesWithFallback();
        }
    }

    private ApiSyncResult prependActivitiesWithFallback() throws Exception {
        try {
            return prependStreamItems();
        } catch (ApiRequestException exception){
            if (exception.isNetworkError()) {
                throw exception;
            } else {
                // we probably had a bad cursor in the local url, so just refresh everything to start over
                return refreshSoundStream();
            }
        }
    }

    private ApiSyncResult refreshSoundStream() throws Exception {
        final ApiRequest.Builder requestBuilder =
                ApiRequest.get(ApiEndpoints.STREAM.path())
                        .addQueryParam(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.LIST_PAGE_SIZE))
                        .forPrivateApi(1);

        ModelCollection<ApiStreamItem> streamItems = apiClient.fetchMappedResponse(requestBuilder.build(), collectionTypeToken);
        replaceSoundStreamCommand.call(getFilteredCollection(streamItems));
        setNextPageUrl(streamItems.getNextLink());

        final Map<String, Link> links = streamItems.getLinks();
        if (links.containsKey(FUTURE_LINK_REL)){
            setFuturePageUrl(links.get(FUTURE_LINK_REL));
        }

        return ApiSyncResult.fromSuccessfulChange(Content.ME_SOUND_STREAM.uri);
    }


    private ApiSyncResult appendStreamItems() throws Exception {
        if (hasNextPageUrl()) {

            final String nextPageUrl = getNextPageUrl();
            Log.d(this, "Building soundstream request from stored next link " + nextPageUrl);

            final ApiRequest.Builder requestBuilder = ApiRequest.get(nextPageUrl).forPrivateApi(1);

            ModelCollection<ApiStreamItem> streamItems = apiClient.fetchMappedResponse(requestBuilder.build(), collectionTypeToken);
            setNextPageUrl(streamItems.getNextLink());

            if (streamItems.getCollection().isEmpty()){
                return ApiSyncResult.fromSuccessWithoutChange(Content.ME_SOUND_STREAM.uri);

            } else {
                storeSoundStreamCommand.call(Iterables.filter(streamItems.getCollection(), removePromotedItemsPredicate));
                return ApiSyncResult.fromSuccessfulChange(Content.ME_SOUND_STREAM.uri);
            }

        } else {

            Log.d(this, "No Next SoundStream page link found. Aborting append");
            return ApiSyncResult.fromSuccessWithoutChange(Content.ME_SOUND_STREAM.uri);
        }
    }

    private ApiSyncResult prependStreamItems() throws Exception {
        final String previousPageUrl = getFuturePageUrl();
        Log.d(this, "Building soundstream request from stored future link " + previousPageUrl);

        final ApiRequest.Builder requestBuilder = ApiRequest.get(previousPageUrl).forPrivateApi(1);

        ModelCollection<ApiStreamItem> streamItems = apiClient.fetchMappedResponse(requestBuilder.build(), collectionTypeToken);
        final Map<String, Link> links = streamItems.getLinks();
        if (links.containsKey(FUTURE_LINK_REL)) {
            setFuturePageUrl(links.get(FUTURE_LINK_REL));
        }

        if (streamItems.getCollection().isEmpty()) {
            return ApiSyncResult.fromSuccessWithoutChange(Content.ME_SOUND_STREAM.uri);
        } else {
            storeSoundStreamCommand.call(getFilteredCollection(streamItems));
            return ApiSyncResult.fromSuccessfulChange(Content.ME_SOUND_STREAM.uri);
        }
    }

    private Iterable<ApiStreamItem> getFilteredCollection(ModelCollection<ApiStreamItem> streamItems) {
        if (flags.isEnabled(Flag.PROMOTED_IN_STREAM)) {
            return streamItems.getCollection();
        } else {
            return Iterables.filter(streamItems.getCollection(), removePromotedItemsPredicate);
        }
    }

    private String getNextPageUrl() {
        return syncPreferences.getString(PREFS_NEXT_URL, ScTextUtils.EMPTY_STRING);
    }

    private boolean hasNextPageUrl() {
        return syncPreferences.contains(PREFS_NEXT_URL);
    }

    private String getFuturePageUrl() {
        return syncPreferences.getString(PREFS_FUTURE_URL, ScTextUtils.EMPTY_STRING);
    }

    private boolean missingFuturePageUrl() {
        return !syncPreferences.contains(PREFS_FUTURE_URL);
    }

    private void setNextPageUrl(Optional<Link> nextLink) {
        if (nextLink.isPresent()) {
            final String href = nextLink.get().getHref();
            Log.d(this, "Writing next soundstream link to preferences : " + href);
            syncPreferences.edit().putString(PREFS_NEXT_URL, href).apply();
        } else {
            Log.d(this, "No next link in soundstream response, clearing any stored link");
            syncPreferences.edit().remove(PREFS_NEXT_URL).apply();
        }
    }

    private void setFuturePageUrl(Link futureLink) {
        final String href = futureLink.getHref();
        Log.d(this, "Writing future soundstream link to preferences : " + href);
        syncPreferences.edit().putString(PREFS_FUTURE_URL, href).apply();
    }

}
