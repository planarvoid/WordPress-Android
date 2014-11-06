package com.soundcloud.android.onboarding;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;

import javax.inject.Inject;

public class OnboardingOperations {

    private final ApiScheduler apiScheduler;

    @Inject
    public OnboardingOperations(ApiScheduler apiScheduler) {
        this.apiScheduler = apiScheduler;
    }

    public void sendEmailOptIn() {
        ApiRequest request = ApiRequest.Builder.put(ApiEndpoints.SUBSCRIPTIONS.path())
                .forPrivateApi(1)
                .withContent(new EmailOptIn())
                .build();
        fireAndForget(apiScheduler.response(request));
    }

    public static class EmailOptIn {

        @JsonProperty
        final boolean newsletter = true;

        @JsonProperty("product_updates")
        final boolean productUpdates = true;

        @JsonProperty
        final boolean surveys = true;

    }
}
