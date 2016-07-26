package com.soundcloud.android.sync.timeline;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;

import android.net.Uri;

import java.util.Map;
import java.util.concurrent.Callable;

// Responsible for syncing features based on the Timeline service, in
// particular the sound stream and the notifications/activities feed.
// See https://github.com/soundcloud/timeline
//
// This class performs bi-directional syncs based on chronological order,
// either backwards in time ("appending" or "backfill" syncs) to support
// lazy paging over a potentially large set of data, or forwards in time
// ("prepending" syncs) to pull in new content from the given timeline
// collection.
public class TimelineSyncer<TimelineModel> implements Callable<Boolean> {

    static final String FUTURE_LINK_REL = "future";
    private static final String TAG = "Timeline";
    private static final int LIMIT = 100;

    private final ApiEndpoints endpoint;
    private final Uri contentUri;
    private final ApiClient apiClient;
    private final Command<Iterable<TimelineModel>, ?> storeItemsCommand;
    private final Command<Iterable<TimelineModel>, ?> replaceItemsCommand;
    private final TimelineSyncStorage timelineSyncStorage;
    private final TypeToken<ModelCollection<TimelineModel>> collectionTypeToken;
    private String action;

    protected TimelineSyncer(ApiEndpoints endpoint, Uri contentUri, ApiClient apiClient,
                             Command<Iterable<TimelineModel>, ?> storeItemsCommand,
                             Command<Iterable<TimelineModel>, ?> replaceItemsCommand,
                             TimelineSyncStorage timelineSyncStorage,
                             TypeToken<ModelCollection<TimelineModel>> collectionTypeToken, String action) {
        this.endpoint = endpoint;
        this.contentUri = contentUri;
        this.apiClient = apiClient;
        this.storeItemsCommand = storeItemsCommand;
        this.replaceItemsCommand = replaceItemsCommand;
        this.timelineSyncStorage = timelineSyncStorage;
        this.collectionTypeToken = collectionTypeToken;
        this.action = action;
    }

    @Override
    public Boolean call() throws Exception {
        log("Syncing with action=" + action);

        if (ApiSyncService.ACTION_APPEND.equals(action)) {
            return append();
        } else if (ApiSyncService.ACTION_HARD_REFRESH.equals(action) || timelineSyncStorage.isMissingFuturePageUrl()) {
            return refresh();
        } else {
            return safePrepend();
        }
    }

    private boolean safePrepend() throws Exception {
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

    private boolean refresh() throws Exception {
        final ApiRequest.Builder requestBuilder =
                ApiRequest.get(endpoint.path())
                          .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, LIMIT)
                          .forPrivateApi();

        ModelCollection<TimelineModel> items = apiClient.fetchMappedResponse(requestBuilder.build(),
                                                                             collectionTypeToken);
        log("New items: " + items.getCollection().size());
        replaceItemsCommand.call(items.getCollection());
        timelineSyncStorage.storeNextPageUrl(items.getNextLink());

        final Map<String, Link> links = items.getLinks();
        if (links.containsKey(FUTURE_LINK_REL)) {
            timelineSyncStorage.storeFuturePageUrl(links.get(FUTURE_LINK_REL));
        }

        return true;
    }

    private boolean append() throws Exception {
        if (timelineSyncStorage.hasNextPageUrl()) {
            final String nextPageUrl = timelineSyncStorage.getNextPageUrl();
            log("Building request from stored next link " + nextPageUrl);

            final ApiRequest.Builder requestBuilder = ApiRequest.get(nextPageUrl).forPrivateApi();

            ModelCollection<TimelineModel> items = apiClient.fetchMappedResponse(requestBuilder.build(),
                                                                                 collectionTypeToken);
            log("New items: " + items.getCollection().size());
            timelineSyncStorage.storeNextPageUrl(items.getNextLink());

            if (items.getCollection().isEmpty()) {
                return false;
            } else {
                storeItemsCommand.call(items.getCollection());
                return true;
            }
        } else {
            log("No next link found. Aborting append.");
            return false;
        }
    }

    private boolean prepend() throws Exception {
        final String futurePageUrl = timelineSyncStorage.getFuturePageUrl();
        log("Building request from stored future link " + futurePageUrl);

        final ApiRequest.Builder requestBuilder = ApiRequest.get(futurePageUrl).forPrivateApi();

        ModelCollection<TimelineModel> items = apiClient.fetchMappedResponse(requestBuilder.build(),
                                                                             collectionTypeToken);
        log("New items: " + items.getCollection().size());
        final Map<String, Link> links = items.getLinks();
        if (links.containsKey(FUTURE_LINK_REL)) {
            timelineSyncStorage.storeFuturePageUrl(links.get(FUTURE_LINK_REL));
        }
        final Command<Iterable<TimelineModel>, ?> insertCommand;
        final Optional<Link> nextLink = items.getNextLink();
        if (nextLink.isPresent()) {
            // if there is a next page of items even when we exhaust our request limit, it
            // means we're at risk creating a gap in the timeline between what has been
            // synced before and what we retrieved, so simply wipe out old data here.
            insertCommand = replaceItemsCommand;
        } else {
            insertCommand = storeItemsCommand;
        }

        if (items.getCollection().isEmpty()) {
            return false;
        } else {
            insertCommand.call(items.getCollection());
            return true;
        }
    }

    private void log(String message) {
        Log.d(TAG, "[" + contentUri.getPath() + "] " + message);
    }
}
