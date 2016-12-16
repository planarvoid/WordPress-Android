package com.soundcloud.android.deeplinks;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Scheduler;

import android.net.Uri;
import android.support.annotation.NonNull;

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

    public Observable<ResolveResult> resolve(@NonNull final Uri originalUri) {
        return Observable.<ResolveResult>create(subscriber -> {
            Uri uri = followClickTrackingUrl(originalUri);
            try {
                ApiResolvedResource resolvedResource = resolveResource(uri.toString());
                final Urn urn = resolvedResource.getUrn();
                if (Urn.NOT_SET.equals(urn)) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(ResolveResult.error(uri, null));
                        subscriber.onCompleted();
                    }
                } else {
                    storeResource(resolvedResource);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(ResolveResult.succes(urn));
                        subscriber.onCompleted();
                    }
                }
            } catch (ApiRequestException | IOException | ApiMapperException e) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(ResolveResult.error(uri, e));
                    subscriber.onCompleted();
                }
            }
        }).subscribeOn(scheduler);
    }

    private ApiResolvedResource resolveResource(@NonNull String identifier)
            throws ApiRequestException, IOException, ApiMapperException {
        ApiRequest request = ApiRequest.get(ApiEndpoints.RESOLVE_ENTITY.path())
                                       .forPrivateApi()
                                       .addQueryParam("identifier", identifier)
                                       .build();

        return apiClient.fetchMappedResponse(request, ApiResolvedResource.class);
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

    private void storeResource(@NonNull ApiResolvedResource resource) {
        if (resource.getOptionalTrack().isPresent()) {
            storeTracksCommand.call(Collections.singletonList(resource.getOptionalTrack().get()));
        } else if (resource.getOptionalPlaylist().isPresent()) {
            storePlaylistsCommand.call(Collections.singletonList(resource.getOptionalPlaylist().get()));
        } else if (resource.getOptionalUser().isPresent()) {
            storeUsersCommand.call(Collections.singletonList(resource.getOptionalUser().get()));
        }
    }
}
