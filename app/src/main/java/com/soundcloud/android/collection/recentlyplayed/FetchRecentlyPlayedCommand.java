package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiRecentlyPlayed;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

class FetchRecentlyPlayedCommand {

    private static final Function<? super ApiRecentlyPlayed, PlayHistoryRecord> TO_PLAY_HISTORY_RECORDS =
            new Function<ApiRecentlyPlayed, PlayHistoryRecord>() {
                public PlayHistoryRecord apply(ApiRecentlyPlayed input) {
                    return PlayHistoryRecord.create(input.getPlayedAt(), Urn.NOT_SET, new Urn(input.getUrn()));
                }
            };

    private final ApiClient apiClient;

    @Inject
    public FetchRecentlyPlayedCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<PlayHistoryRecord> call() throws ApiRequestException, IOException, ApiMapperException {
        return transform(getRecentlyPlayed().getCollection(), TO_PLAY_HISTORY_RECORDS);
    }

    private ModelCollection<ApiRecentlyPlayed> getRecentlyPlayed() throws IOException, ApiRequestException, ApiMapperException {
        ApiRequest request = ApiRequest.get(ApiEndpoints.RECENTLY_PLAYED.path())
                                       .forPrivateApi()
                                       .build();

        return apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiRecentlyPlayed>>() {
        });
    }
}
