package com.soundcloud.android.onboarding;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

public class OnboardingOperations {

    public static final String ONBOARDING_TAG = "ScOnboarding";

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;

    @Inject
    public OnboardingOperations(ApiClientRx apiClientRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
    }

    public void sendEmailOptIn() {
        ApiRequest request = ApiRequest.put(ApiEndpoints.SUBSCRIPTIONS.path())
                .forPrivateApi(1)
                .withContent(new EmailOptIn())
                .build();
        fireAndForget(apiClientRx.response(request).subscribeOn(scheduler));
    }

    static class EmailOptIn {

        @JsonProperty
        final boolean newsletter = true;

        @JsonProperty("product_updates")
        final boolean productUpdates = true;

        @JsonProperty
        final boolean surveys = true;

    }
}
