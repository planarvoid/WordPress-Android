package com.soundcloud.android.profile;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.rx.ScSchedulers;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

class UpdateAgeCommand extends LegacyCommand<BirthdayInfo, Boolean, UpdateAgeCommand> {

    private final ApiClient apiClient;

    @Inject
    public UpdateAgeCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Boolean call() throws Exception {
        Map<String, Integer> body = new HashMap<>(2);
        body.put("month", input.getMonth());
        body.put("year", input.getYear());

        ApiRequest request = ApiRequest.put(ApiEndpoints.MY_DOB.path()).forPrivateApi().withContent(body).build();
        ApiResponse response = apiClient.fetchResponse(request);
        return response.isSuccess();
    }

    public void call(BirthdayInfo birthday, Subscriber<Boolean> responseHandler) {
        call(birthday).subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER).observeOn(AndroidSchedulers.mainThread()).subscribe(responseHandler);
    }
}
