package com.soundcloud.android.deeplinks;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Collections;

class ResolveOperations {
    private final ApiClient apiClient;
    private final Scheduler scheduler;
    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    ResolveOperations(ApiClient apiClient,
                      @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                      StoreTracksCommand storeTracksCommand,
                      StorePlaylistsCommand storePlaylistsCommand,
                      StoreUsersCommand storeUsersCommand) {
        this.apiClient = apiClient;
        this.scheduler = scheduler;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
    }

    public Observable<PublicApiResource> resolve(@NonNull final Uri originalUri) {
        return Observable.create(new Observable.OnSubscribe<PublicApiResource>() {
            @Override
            public void call(Subscriber<? super PublicApiResource> subscriber) {
                Uri uri = followClickTrackingUrl(originalUri);
                PublicApiResource resolvedResource = resolveUri(uri);
                if (resolvedResource != null) {
                    storeResource(resolvedResource);
                    subscriber.onNext(resolvedResource);
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(new OnErrorThrowable.OnNextValue(uri));
                }
            }
        }).subscribeOn(scheduler);
    }

    @Nullable
    private PublicApiResource resolveUri(@NonNull Uri uri) {
        Urn urn = resolveUrn(uri);

        if (Urn.NOT_SET.equals(urn)) {
            return resolveResource(uri.toString()); // use resolver with url
        } else {
            return resolveResource(urn.toString()); // use resolver with urn
        }
    }

    @Nullable
    private PublicApiResource resolveResource(@NonNull String url) {
        try {
            // Note: OkHttp will follow redirects as needed
            ApiRequest request = ApiRequest.get(ApiEndpoints.RESOLVE.path())
                    .forPublicApi()
                    .addQueryParam("url", url)
                    .build();

            return apiClient.fetchMappedResponse(request, PublicApiResource.class);
        } catch (IOException | ApiMapperException | ApiRequestException e) {
            return null;
        }
    }

    @NonNull
    private Uri followClickTrackingUrl(@NonNull Uri uri) {
        if (DeepLink.isClickTrackingUrl(uri)) {
            // Just hit the click tracking url and extract the url
            apiClient.fetchResponse(ApiRequest.get(uri.toString()).forPublicApi().build());
            return DeepLink.extractClickTrackingRedirectUrl(uri);
        } else {
            return uri;
        }
    }

    @NonNull
    private Urn resolveUrn(@NonNull Uri uri) {
        try {
            UrnResolver resolver = new UrnResolver();
            return resolver.toUrn(uri);
        } catch (IllegalArgumentException e) {
            return Urn.NOT_SET;
        }
    }

    private void storeResource(@NonNull PublicApiResource resource) {
        Urn urn = resource.getUrn();

        if (urn.isTrack()) {
            storeTracksCommand.call(Collections.singletonList(((PublicApiTrack) resource).toApiMobileTrack()));
        } else if (urn.isPlaylist()) {
            storePlaylistsCommand.call(Collections.singletonList(((PublicApiPlaylist) resource).toApiMobilePlaylist()));
        } else if (urn.isUser()) {
            storeUsersCommand.call(Collections.singletonList(((PublicApiUser) resource).toApiMobileUser()));
        }
    }
}
