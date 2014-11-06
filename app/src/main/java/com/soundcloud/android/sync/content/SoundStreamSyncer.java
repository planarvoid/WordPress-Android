package com.soundcloud.android.sync.content;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.stream.SoundStreamWriteStorage;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import javax.inject.Inject;
import java.io.IOException;

public class SoundStreamSyncer implements SyncStrategy {

    private final ApiClient apiClient;
    private final SoundStreamWriteStorage writeStorage;
    private TypeToken<ModelCollection<ApiStreamItem>> collectionTypeToken = new TypeToken<ModelCollection<ApiStreamItem>>() { };
    private Predicate<ApiStreamItem> removePromotedItemsPredicate = new Predicate<ApiStreamItem>() {
        @Override
        public boolean apply(ApiStreamItem input) {
            return !input.isPromotedStreamItem();
        }
    };

    @Inject
    public SoundStreamSyncer(ApiClient apiClient, SoundStreamWriteStorage writeStorage) {
        this.apiClient = apiClient;
        this.writeStorage = writeStorage;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws IOException, ApiMapperException {
        Log.d("syncActivities(" + uri + "); action=" + action);

        if (ApiSyncService.ACTION_HARD_REFRESH.equals(action)) {
            return refreshSoundStream();
        }
        return new ApiSyncResult(uri);
    }

    private ApiSyncResult refreshSoundStream() throws IOException, ApiMapperException {

        final ApiRequest.Builder<ModelCollection<ApiStreamItem>> requestBuilder =
                ApiRequest.Builder.<ModelCollection<ApiStreamItem>>get(ApiEndpoints.STREAM.path())
                        .forResource(collectionTypeToken)
                        .forPrivateApi(1);

        ModelCollection<ApiStreamItem> streamItems = apiClient.fetchMappedResponse(requestBuilder.build());
        writeStorage.replaceStreamItems(Iterables.filter(streamItems.getCollection(), removePromotedItemsPredicate));

        return ApiSyncResult.fromSuccessfulChange(Content.ME_SOUND_STREAM.uri);
    }
}
