package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.commands.Command;

import javax.inject.Inject;

class ClearPlayHistoryCommand extends Command<Void, Boolean> {

    private final PlayHistoryStorage storage;
    private final ApiClient apiClient;

    @Inject
    ClearPlayHistoryCommand(PlayHistoryStorage storage,
                                   ApiClient apiClient) {
        this.storage = storage;
        this.apiClient = apiClient;
    }

    @Override
    public Boolean call(Void input) {
        ApiRequest request = ApiRequest.delete(ApiEndpoints.CLEAR_PLAY_HISTORY.path())
                                       .forPrivateApi()
                                       .build();

        ApiResponse apiResponse = apiClient.fetchResponse(request);
        boolean wasSuccessful = apiResponse.isSuccess();

        if (wasSuccessful) {
            storage.clear();
        }

        return wasSuccessful;
    }
}
