package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.model.ApiRecentlyPlayed;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.commands.Command;
import com.soundcloud.java.functions.Function;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PushRecentlyPlayedCommand extends Command<PlayHistoryRecord, List<PlayHistoryRecord>> {

    private static final Function<PlayHistoryRecord, ApiRecentlyPlayed> TO_API_RECENTLY_PLAYED =
            input -> ApiRecentlyPlayed.create(input.timestamp(), input.contextUrn().toString());

    private final RecentlyPlayedStorage recentlyPlayedStorage;
    private final ApiClient apiClient;

    @Inject
    public PushRecentlyPlayedCommand(RecentlyPlayedStorage recentlyPlayedStorage,
                              ApiClient apiClient) {
        this.recentlyPlayedStorage = recentlyPlayedStorage;
        this.apiClient = apiClient;
    }

    @Override
    public List<PlayHistoryRecord> call(PlayHistoryRecord ignore) {
        List<PlayHistoryRecord> unSyncedRecords = recentlyPlayedStorage.loadUnSyncedRecentlyPlayed();

        if (!unSyncedRecords.isEmpty()) {
            pushRecentlyPlayed(unSyncedRecords);
        }

        return unSyncedRecords;
    }

    private void pushRecentlyPlayed(List<PlayHistoryRecord> unSyncedRecords) {
        ApiRequest request = ApiRequest.post(ApiEndpoints.RECENTLY_PLAYED.path())
                                       .withContent(buildRecentlyPlayedCollection(unSyncedRecords))
                                       .forPrivateApi()
                                       .build();

        ApiResponse response = apiClient.fetchResponse(request);

        if (response.isSuccess()) {
            recentlyPlayedStorage.setSynced(unSyncedRecords);
        }
    }

    private ModelCollection<ApiRecentlyPlayed> buildRecentlyPlayedCollection(List<PlayHistoryRecord> unSyncedRecords) {
        Collection<ApiRecentlyPlayed> playHistory = transform(unSyncedRecords, TO_API_RECENTLY_PLAYED);
        return new ModelCollection<>(new ArrayList<>(playHistory));
    }
}
