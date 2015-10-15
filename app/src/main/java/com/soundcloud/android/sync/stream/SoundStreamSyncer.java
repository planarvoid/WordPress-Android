package com.soundcloud.android.sync.stream;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.utils.LocaleProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import javax.inject.Inject;
import java.util.Map;

public class SoundStreamSyncer implements SyncStrategy {

    static final String FUTURE_LINK_REL = "future";

    private final ApiClient apiClient;
    private final StoreSoundStreamCommand storeSoundStreamCommand;
    private final ReplaceSoundStreamCommand replaceSoundStreamCommand;
    private final StreamSyncStorage streamSyncStorage;
    private final TypeToken<ModelCollection<ApiStreamItem>> collectionTypeToken = new TypeToken<ModelCollection<ApiStreamItem>>() {
    };

    @Inject
    public SoundStreamSyncer(ApiClient apiClient, StoreSoundStreamCommand storeSoundStreamCommand,
                             ReplaceSoundStreamCommand replaceSoundStreamCommand, StreamSyncStorage streamSyncStorage) {
        this.apiClient = apiClient;
        this.storeSoundStreamCommand = storeSoundStreamCommand;
        this.replaceSoundStreamCommand = replaceSoundStreamCommand;
        this.streamSyncStorage = streamSyncStorage;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws Exception {
        Log.d(this, "syncActivities(" + uri + "); action=" + action);

        if (ApiSyncService.ACTION_APPEND.equals(action)) {
            return appendStreamItems();
        } else if (ApiSyncService.ACTION_HARD_REFRESH.equals(action) || streamSyncStorage.isMissingFuturePageUrl()) {
            return refreshSoundStream();
        } else {
            return prependActivitiesWithFallback();
        }
    }

    private ApiSyncResult prependActivitiesWithFallback() throws Exception {
        try {
            return prependStreamItems();
        } catch (ApiRequestException exception) {
            if (!exception.isNetworkError()) {
                // we may have had a bad cursor in the future url, so clear it for the next sync
                streamSyncStorage.clear();
            }
            throw exception;
        }
    }

    private ApiSyncResult refreshSoundStream() throws Exception {
        final ApiRequest.Builder requestBuilder =
                ApiRequest.get(ApiEndpoints.STREAM.path())
                        .addQueryParam(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.LIST_PAGE_SIZE))
                        .forPrivateApi(1);

        final String locale = LocaleProvider.getFormattedLocale();
        if (!locale.isEmpty()) {
            requestBuilder.addQueryParam(ApiRequest.Param.LOCALE, locale);
        }

        ModelCollection<ApiStreamItem> streamItems = apiClient.fetchMappedResponse(requestBuilder.build(), collectionTypeToken);
        replaceSoundStreamCommand.call(streamItems.getCollection());
        streamSyncStorage.storeNextPageUrl(streamItems.getNextLink());

        final Map<String, Link> links = streamItems.getLinks();
        if (links.containsKey(FUTURE_LINK_REL)) {
            streamSyncStorage.storeFuturePageUrl(links.get(FUTURE_LINK_REL));
        }

        return ApiSyncResult.fromSuccessfulChange(Content.ME_SOUND_STREAM.uri);
    }

    private ApiSyncResult appendStreamItems() throws Exception {
        if (streamSyncStorage.hasNextPageUrl()) {

            final String nextPageUrl = streamSyncStorage.getNextPageUrl();
            Log.d(this, "Building soundstream request from stored next link " + nextPageUrl);

            final ApiRequest.Builder requestBuilder = ApiRequest.get(nextPageUrl).forPrivateApi(1);

            ModelCollection<ApiStreamItem> streamItems = apiClient.fetchMappedResponse(requestBuilder.build(), collectionTypeToken);
            streamSyncStorage.storeNextPageUrl(streamItems.getNextLink());

            if (streamItems.getCollection().isEmpty()) {
                return ApiSyncResult.fromSuccessWithoutChange(Content.ME_SOUND_STREAM.uri);

            } else {
                storeSoundStreamCommand.call(streamItems.getCollection());
                return ApiSyncResult.fromSuccessfulChange(Content.ME_SOUND_STREAM.uri);
            }

        } else {

            Log.d(this, "No Next SoundStream page link found. Aborting append");
            return ApiSyncResult.fromSuccessWithoutChange(Content.ME_SOUND_STREAM.uri);
        }
    }

    private ApiSyncResult prependStreamItems() throws Exception {
        final String previousPageUrl = streamSyncStorage.getFuturePageUrl();
        Log.d(this, "Building soundstream request from stored future link " + previousPageUrl);

        final ApiRequest.Builder requestBuilder = ApiRequest.get(previousPageUrl).forPrivateApi(1);

        ModelCollection<ApiStreamItem> streamItems = apiClient.fetchMappedResponse(requestBuilder.build(), collectionTypeToken);
        final Map<String, Link> links = streamItems.getLinks();
        if (links.containsKey(FUTURE_LINK_REL)) {
            streamSyncStorage.storeFuturePageUrl(links.get(FUTURE_LINK_REL));
        }

        if (streamItems.getCollection().isEmpty()) {
            return ApiSyncResult.fromSuccessWithoutChange(Content.ME_SOUND_STREAM.uri);
        } else {
            storeSoundStreamCommand.call(streamItems.getCollection());
            return ApiSyncResult.fromSuccessfulChange(Content.ME_SOUND_STREAM.uri);
        }
    }

}
