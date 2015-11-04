package com.soundcloud.android.sync.timeline;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncStrategy;
import com.soundcloud.android.utils.LocaleProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import java.util.Map;

// Responsible for syncing features based on the Timeline service, in
// particular the sound stream and the notifications/activities feed.
// See https://github.com/soundcloud/timeline
//
// This class performs bi-directional syncs based on chronological order,
// either backwards in time ("appending" or "backfill" syncs) to support
// lazy paging over a potentially large set of data, or forwards in time
// ("prepending" syncs) to pull in new content from the given timeline
// collection.
public class TimelineSyncer<TimelineModel> implements SyncStrategy {

    static final String FUTURE_LINK_REL = "future";

    private final ApiEndpoints endpoint;
    private final Uri contentUri;
    private final ApiClient apiClient;
    private final Command<Iterable<TimelineModel>, ?> storeItemsCommand;
    private final Command<Iterable<TimelineModel>, ?> replaceItemsCommand;
    private final TimelineSyncStorage timelineSyncStorage;
    private final TypeToken<ModelCollection<TimelineModel>> collectionTypeToken;

    protected TimelineSyncer(ApiEndpoints endpoint, Uri contentUri, ApiClient apiClient,
                             Command<Iterable<TimelineModel>, ?> storeItemsCommand,
                             Command<Iterable<TimelineModel>, ?> replaceItemsCommand,
                             TimelineSyncStorage timelineSyncStorage,
                             TypeToken<ModelCollection<TimelineModel>> collectionTypeToken) {
        this.endpoint = endpoint;
        this.contentUri = contentUri;
        this.apiClient = apiClient;
        this.storeItemsCommand = storeItemsCommand;
        this.replaceItemsCommand = replaceItemsCommand;
        this.timelineSyncStorage = timelineSyncStorage;
        this.collectionTypeToken = collectionTypeToken;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        Log.d(this, "Syncing " + uri + "; action=" + action);

        if (ApiSyncService.ACTION_APPEND.equals(action)) {
            return append();
        } else if (ApiSyncService.ACTION_HARD_REFRESH.equals(action) || timelineSyncStorage.isMissingFuturePageUrl()) {
            return refresh();
        } else {
            return safePrepend();
        }
    }

    private ApiSyncResult safePrepend() throws Exception {
        try {
            return prepend();
        } catch (ApiRequestException exception) {
            if (!exception.isNetworkError()) {
                // we may have had a bad cursor in the future url, so clear it for the next sync
                timelineSyncStorage.clear();
            }
            throw exception;
        }
    }

    private ApiSyncResult refresh() throws Exception {
        final ApiRequest.Builder requestBuilder =
                ApiRequest.get(endpoint.path())
                        .addQueryParam(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.LIST_PAGE_SIZE))
                        .forPrivateApi(1);

        final String locale = LocaleProvider.getFormattedLocale();
        if (!locale.isEmpty()) {
            requestBuilder.addQueryParam(ApiRequest.Param.LOCALE, locale);
        }

        ModelCollection<TimelineModel> streamItems = apiClient.fetchMappedResponse(requestBuilder.build(),
                collectionTypeToken);
        replaceItemsCommand.call(streamItems.getCollection());
        timelineSyncStorage.storeNextPageUrl(streamItems.getNextLink());

        final Map<String, Link> links = streamItems.getLinks();
        if (links.containsKey(FUTURE_LINK_REL)) {
            timelineSyncStorage.storeFuturePageUrl(links.get(FUTURE_LINK_REL));
        }

        return ApiSyncResult.fromSuccessfulChange(contentUri);
    }

    private ApiSyncResult append() throws Exception {
        if (timelineSyncStorage.hasNextPageUrl()) {

            final String nextPageUrl = timelineSyncStorage.getNextPageUrl();
            Log.d(this, "Building request from stored next link " + nextPageUrl);

            final ApiRequest.Builder requestBuilder = ApiRequest.get(nextPageUrl).forPrivateApi(1);

            ModelCollection<TimelineModel> streamItems = apiClient.fetchMappedResponse(requestBuilder.build(),
                    collectionTypeToken);
            timelineSyncStorage.storeNextPageUrl(streamItems.getNextLink());

            if (streamItems.getCollection().isEmpty()) {
                return ApiSyncResult.fromSuccessWithoutChange(contentUri);

            } else {
                storeItemsCommand.call(streamItems.getCollection());
                return ApiSyncResult.fromSuccessfulChange(contentUri);
            }

        } else {

            Log.d(this, "No next link found. Aborting append.");
            return ApiSyncResult.fromSuccessWithoutChange(contentUri);
        }
    }

    private ApiSyncResult prepend() throws Exception {
        final String previousPageUrl = timelineSyncStorage.getFuturePageUrl();
        Log.d(this, "Building request from stored future link " + previousPageUrl);

        final ApiRequest.Builder requestBuilder = ApiRequest.get(previousPageUrl).forPrivateApi(1);

        ModelCollection<TimelineModel> items = apiClient.fetchMappedResponse(requestBuilder.build(),
                collectionTypeToken);
        final Map<String, Link> links = items.getLinks();
        if (links.containsKey(FUTURE_LINK_REL)) {
            timelineSyncStorage.storeFuturePageUrl(links.get(FUTURE_LINK_REL));
        }

        if (items.getCollection().isEmpty()) {
            return ApiSyncResult.fromSuccessWithoutChange(contentUri);
        } else {
            storeItemsCommand.call(items.getCollection());
            return ApiSyncResult.fromSuccessfulChange(contentUri);
        }
    }

}
