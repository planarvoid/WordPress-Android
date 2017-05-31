package com.soundcloud.android.navigation;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.deeplinks.DeepLink;
import com.soundcloud.android.model.Urn;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import android.net.Uri;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Collections;

class ResolveOperations {

    private final ApiClientRxV2 apiClient;
    private final Scheduler scheduler;
    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    ResolveOperations(ApiClientRxV2 apiClient,
                      @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                      StoreTracksCommand storeTracksCommand,
                      StorePlaylistsCommand storePlaylistsCommand,
                      StoreUsersCommand storeUsersCommand) {
        this.apiClient = apiClient;
        this.scheduler = scheduler;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
    }

    Single<ResolveResult> resolve(@NonNull final String target) {
        final Uri originalUri = Uri.parse(target);
        return followClickTrackingUrl(originalUri)
                .flatMap(uri -> Single.zip(
                        Single.just(uri),
                        resolveResource(uri.toString()),
                        (uri1, resolvedUrn) -> {
                            if (Urn.NOT_SET.equals(resolvedUrn)) {
                                return ResolveResult.error(uri1, null);
                            } else {
                                return ResolveResult.succes(resolvedUrn);
                            }
                        }).onErrorReturn(e -> ResolveResult.error(uri, new IOException(e)))
                )
                .subscribeOn(scheduler);
    }

    private Single<Urn> resolveResource(@NonNull String identifier)
            throws ApiRequestException, IOException, ApiMapperException {
        return resolveViaApi(identifier).doOnSuccess(this::storeResource)
                                        .map(ApiResolvedResource::getUrn);
    }

    private Single<ApiResolvedResource> resolveViaApi(@NonNull String identifier) {
        ApiRequest request = ApiRequest.get(ApiEndpoints.RESOLVE_ENTITY.path())
                                       .forPrivateApi()
                                       .addQueryParam("identifier", identifier)
                                       .build();
        return apiClient.mappedResponse(request, ApiResolvedResource.class);
    }

    private Single<Uri> followClickTrackingUrl(@NonNull Uri uri) {
        if (DeepLink.isClickTrackingUrl(uri)) {
            // Just hit the click tracking url and extract the url
            return apiClient.ignoreResultRequest(ApiRequest.get(uri.toString()).forPublicApi().build())
                            .toSingleDefault(DeepLink.extractClickTrackingRedirectUrl(uri))
                            .onErrorReturn(throwable -> DeepLink.extractClickTrackingRedirectUrl(uri));
        } else {
            return Single.just(uri);
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
