package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.model.ApiPlayHistory;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.java.functions.Function;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class PushPlayHistoryCommand extends Command<PlayHistoryRecord, List<PlayHistoryRecord>> {

    private static final Function<PlayHistoryRecord, ApiPlayHistory> TO_API_PLAY_HISTORY =
            input -> ApiPlayHistory.create(input.timestamp(), input.trackUrn().toString());

    private final PlayHistoryStorage playHistoryStorage;
    private final ApiClient apiClient;

    @Inject
    PushPlayHistoryCommand(PlayHistoryStorage playHistoryStorage,
                           ApiClient apiClient) {
        this.playHistoryStorage = playHistoryStorage;
        this.apiClient = apiClient;
    }

    @Override
    public List<PlayHistoryRecord> call(PlayHistoryRecord ignore) {
        List<PlayHistoryRecord> unSyncedRecords = playHistoryStorage.loadUnSyncedPlayHistory();

        if (!unSyncedRecords.isEmpty()) {
            pushPlayHistory(unSyncedRecords);
        }

        return unSyncedRecords;
    }

    private void pushPlayHistory(List<PlayHistoryRecord> unSyncedRecords) {
        ApiRequest request = ApiRequest.post(ApiEndpoints.PLAY_HISTORY.path())
                                       .withContent(buildPlayHistoryCollection(unSyncedRecords))
                                       .forPrivateApi()
                                       .build();

        ApiResponse response = apiClient.fetchResponse(request);

        if (response.isSuccess()) {
            playHistoryStorage.setSynced(unSyncedRecords);
        }
    }

    private ModelCollection<ApiPlayHistory> buildPlayHistoryCollection(List<PlayHistoryRecord> unSyncedRecords) {
        Collection<ApiPlayHistory> playHistory = transform(unSyncedRecords, TO_API_PLAY_HISTORY);
        return new ModelCollection<>(new ArrayList<>(playHistory));
    }
}
