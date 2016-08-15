package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiPlayHistory;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

class FetchPlayHistoryCommand {

    private static final Function<? super ApiPlayHistory, PlayHistoryRecord> TO_PLAY_HISTORY_RECORDS =
            new Function<ApiPlayHistory, PlayHistoryRecord>() {
                public PlayHistoryRecord apply(ApiPlayHistory input) {
                    return PlayHistoryRecord.create(input.getPlayedAt(), new Urn(input.getUrn()), Urn.NOT_SET);
                }
            };

    private final ApiClient apiClient;

    @Inject
    public FetchPlayHistoryCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<PlayHistoryRecord> call() throws ApiRequestException, IOException, ApiMapperException {
        return transform(getPlayHistory().getCollection(), TO_PLAY_HISTORY_RECORDS);
    }

    private ModelCollection<ApiPlayHistory> getPlayHistory() throws IOException, ApiRequestException, ApiMapperException {
        ApiRequest request = ApiRequest.get(ApiEndpoints.PLAY_HISTORY.path())
                                       .forPrivateApi()
                                       .build();

        return apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiPlayHistory>>() {
        });
    }
}
