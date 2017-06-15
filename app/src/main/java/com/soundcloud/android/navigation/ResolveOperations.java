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
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.stations.StationsStorage;
import com.soundcloud.android.stations.StoreStationCommand;
import com.soundcloud.android.tracks.TrackStorage;
import com.soundcloud.android.users.UserStorage;
import com.soundcloud.android.utils.UriUtils;

import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

class ResolveOperations {

    private final ApiClientRxV2 apiClient;
    private final Scheduler scheduler;
    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;
    private final StoreStationCommand storeStationsCommand;

    private final TrackStorage trackStorage;
    private final PlaylistStorage playlistStorage;
    private final UserStorage userStorage;
    private final StationsStorage stationsStorage;

    @Inject
    ResolveOperations(ApiClientRxV2 apiClient,
                      @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                      StoreTracksCommand storeTracksCommand,
                      StorePlaylistsCommand storePlaylistsCommand,
                      StoreUsersCommand storeUsersCommand,
                      StoreStationCommand storeStationsCommand,
                      TrackStorage trackStorage,
                      PlaylistStorage playlistStorage,
                      UserStorage userStorage,
                      StationsStorage stationsStorage) {
        this.apiClient = apiClient;
        this.scheduler = scheduler;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.storeStationsCommand = storeStationsCommand;
        this.trackStorage = trackStorage;
        this.playlistStorage = playlistStorage;
        this.userStorage = userStorage;
        this.stationsStorage = stationsStorage;
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
                                return ResolveResult.success(resolvedUrn);
                            }
                        }).onErrorReturn(e -> ResolveResult.error(uri, new IOException(e)))
                )
                .subscribeOn(scheduler);
    }

    private Single<Urn> resolveResource(@NonNull String identifier)
            throws ApiRequestException, IOException, ApiMapperException {
        return resolveViaStorage(identifier).toSingle()
                                            .onErrorResumeNext(error -> {
                                                if (error instanceof NoSuchElementException) {
                                                    return resolveViaApi(identifier).doOnSuccess(this::storeResource)
                                                                                    .map(ApiResolvedResource::getUrn);
                                                } else {
                                                    return Single.error(error);
                                                }
                                            });
    }

    private Maybe<Urn> resolveViaStorage(@NonNull String identifier) {
        final String permalink = extractPermalink(identifier);
        if (isTrackPermalink(permalink)) {
            return trackStorage.urnForPermalink(permalink);
        } else if (isPlaylistPermalink(permalink)) {
            return playlistStorage.urnForPermalink(permalink);
        } else if (isUserPermalink(permalink)) {
            return userStorage.urnForPermalink(permalink);
        } else if (isStationsPermalink(permalink)) {
            return stationsStorage.urnForPermalink(permalink);
        } else {
            return Maybe.empty();
        }
    }

    @VisibleForTesting
    String extractPermalink(String identifier) {
        final Uri uri = UriUtils.convertToHierarchicalUri(Uri.parse(identifier));
        final String permalink;
        if (DeepLink.isHierarchicalSoundCloudScheme(uri)) {
            permalink = uri.getHost() + uri.getPath();
        } else {
            permalink = uri.getPath().substring(1);
        }
        return permalink;
    }

    @VisibleForTesting
    boolean isStationsPermalink(@NonNull String permalink) {
        final List<String> segments = Uri.parse(permalink).getPathSegments();
        return segments.size() >= 3 && segments.get(0).equals("stations") && (segments.get(1).equals("artist") || segments.get(1).equals("track"));
    }

    @VisibleForTesting
    boolean isUserPermalink(@NonNull String permalink) {
        return Uri.parse(permalink).getPathSegments().size() == 1;
    }

    @VisibleForTesting
    boolean isPlaylistPermalink(@NonNull String permalink) {
        final List<String> segments = Uri.parse(permalink).getPathSegments();
        return segments.size() == 3 && segments.get(1).equals("sets");
    }

    @VisibleForTesting
    boolean isTrackPermalink(@NonNull String permalink) {
        return Uri.parse(permalink).getPathSegments().size() == 2;
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
        } else if (resource.getOptionalStation().isPresent()) {
            storeStationsCommand.call(resource.getOptionalStation().get());
        }
    }
}
