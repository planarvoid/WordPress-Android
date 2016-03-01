package com.soundcloud.android.accounts;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.io.IOException;

public class FetchMeCommand extends Command<Void, Me> {

    private static final String TAG = "FetchMeCommand";
    private final ApiClient apiClient;

    @Inject
    public FetchMeCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Me call(Void input) {
        try {
            return apiClient.fetchMappedResponse(buildRequest(), new TypeToken<Me>() {});
        } catch (IOException | ApiRequestException | ApiMapperException e) {
            Log.e(TAG, "Error fetching me", e);
        }
        return null;
    }

    protected ApiRequest buildRequest() {
        return ApiRequest.get(ApiEndpoints.ME.path())
                .forPrivateApi(1)
                .build();
    }
}
