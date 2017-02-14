package com.soundcloud.android.analytics.firebase;

import static com.soundcloud.android.analytics.firebase.FirebaseModule.FIREBASE_HTTP_CLIENT;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.FirebaseOptions;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.java.strings.Charsets;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import rx.Observable;
import rx.Scheduler;

import android.content.res.Resources;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Used to interact with the Firebase Dynamic Links REST API.
 *
 * @see <a href="https://firebase.google.com/docs/dynamic-links/">Firebase Dynamic Links</a>
 * @see <a href="https://firebase.google.com/docs/dynamic-links/create-links#using_the_rest_api">Firebase Dynamic Links REST API</a>
 */
public class FirebaseDynamicLinksApi {

    /**
     * @see <a href="https://console.firebase.google.com/project/soundcloud.com:soundcloud/settings/general/android:com.soundcloud.android">Web API Key</a>
     */
    private static final String DYNAMIC_LINK_API_URL = "https://firebasedynamiclinks.googleapis.com/v1/shortLinks";

    /**
     * @see <a href="https://console.firebase.google.com/project/soundcloud.com:soundcloud/durablelinks/links/">Firebase Console</a>
     */
    private static final String APP_CODE = "soundcloud.app.goo.gl";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private final FirebaseOptions firebaseOptions;
    private final Resources resources;
    private final OkHttpClient httpClient;
    private final Scheduler scheduler;

    /**
     * Successful response received when a new Dynamic Link has been created.
     */
    private static class CreateDynamicLinkResponse {
        final String shortLink;

        @JsonCreator
        CreateDynamicLinkResponse(@JsonProperty("shortLink") String shortLink) {
            this.shortLink = checkNotNull(shortLink, "shortLink");
        }
    }

    @Inject
    FirebaseDynamicLinksApi(
            FirebaseOptions firebaseOptions,
            Resources resources,
            @Named(FIREBASE_HTTP_CLIENT) OkHttpClient httpClient,
            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.firebaseOptions = firebaseOptions;
        this.resources = resources;
        this.httpClient = httpClient;
        this.scheduler = scheduler;
    }

    /**
     * Returns the result of requesting a Firebase Dynamic Link for the specified URL. Network errors and failure responses
     * will be handled in {@link rx.Observer#onError(Throwable)} as {@link IOException IOExceptions}.
     */
    public Observable<String> createDynamicLink(String originalUrl) {
        return Observable.fromCallable(() -> {
            Request request = buildCreateDynamicLinkRequest(originalUrl);
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected Firebase response: " + response + " Body: " + response.body().string());
            }
            return JacksonJsonTransformer.buildObjectMapper().readValue(response.body().byteStream(), CreateDynamicLinkResponse.class).shortLink;
        }).subscribeOn(scheduler);
    }

    private Request buildCreateDynamicLinkRequest(String originalUrl) throws JSONException, UnsupportedEncodingException {
        // Always use the production package so that developer links can still be used to test
        // the Play store interaction.
        JSONObject androidInfoJson = new JSONObject()
                .put("androidPackageName", resources.getString(R.string.root_package));

        JSONObject dynamicLinkInfoJson = new JSONObject()
                .put("dynamicLinkDomain", APP_CODE)
                .put("link", originalUrl)
                .put("androidInfo", androidInfoJson);

        // Use the shortest URL possible. The alternative is to use long URLs that are harder to guess.
        JSONObject suffixJson = new JSONObject()
                .put("option", "SHORT");

        JSONObject requestJson = new JSONObject()
                .put("suffix", suffixJson)
                .put("dynamicLinkInfo", dynamicLinkInfoJson);

        final Request.Builder builder = new Request.Builder();
        builder.url(DYNAMIC_LINK_API_URL + "?key=" + firebaseOptions.getApiKey());
        RequestBody result = RequestBody.create(MediaType.parse(JSON_CONTENT_TYPE), requestJson.toString().getBytes(Charsets.UTF_8.name()));
        builder.post(result);
        return builder.build();
    }

}
