package com.soundcloud.android.onboarding;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;

import javax.inject.Inject;

public class OnboardingOperations {

    private final RxHttpClient rxHttpClient;

    @Inject
    public OnboardingOperations(RxHttpClient rxHttpClient) {
        this.rxHttpClient = rxHttpClient;
    }

    public void sendEmailOptIn() {
        APIRequest<Void> request = SoundCloudAPIRequest.RequestBuilder.<Void>put(APIEndpoints.SUBSCRIPTIONS.path())
                .forPrivateAPI(1)
                .withContent(new EmailOptIn())
                .build();
        fireAndForget(rxHttpClient.fetchResponse(request));
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
