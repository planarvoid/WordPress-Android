package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.commands.Command;

import javax.inject.Inject;

public class ClearRecentlyPlayedCommand extends Command<Void, Boolean> {

    private final ApiClient apiClient;
    private final RecentlyPlayedStorage storage;

    @Inject
    ClearRecentlyPlayedCommand(ApiClient apiClient, RecentlyPlayedStorage storage) {
        this.apiClient = apiClient;
        this.storage = storage;
    }

    @Override
    public Boolean call(Void input) {
        final ApiRequest request = ApiRequest.delete(ApiEndpoints.CLEAR_RECENTLY_PLAYED.path())
                .forPrivateApi()
                .build();

        final ApiResponse apiResponse = apiClient.fetchResponse(request);
        final boolean success = apiResponse.isSuccess();

        if (success) {
            storage.clear();
        }

        return success;
    }
}
