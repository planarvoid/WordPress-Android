package com.soundcloud.android.navigation;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StoreStationCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.net.Uri;

import java.io.IOException;
import java.util.Collections;

public class ResolveOperationsTest extends AndroidUnitTest {

    private ResolveOperations operations;

    @Mock private ApiClientRxV2 apiClient;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private StoreStationCommand storeStationsCommand;

    private Scheduler scheduler = Schedulers.trampoline();

    @Before
    public void setUp() throws Exception {
        operations = new ResolveOperations(apiClient,
                                           scheduler,
                                           storeTracksCommand,
                                           storePlaylistsCommand,
                                           storeUsersCommand,
                                           storeStationsCommand);
    }

    @Test
    public void fetchesResourceViaResolveEndpoint() throws Exception {
        String uri = "http://soundcloud.com/skrillex";
        ApiResolvedResource resolvedResource = resolvedTrack();
        mockResolutionFor(uri, resolvedResource);

        operations.resolve(uri).test()
                  .assertValue(ResolveResult.succes(resolvedResource.getOptionalTrack().get().getUrn()))
                  .assertComplete();
    }

    @Test
    public void shouldReportUnsupportedResourcesAsError() throws Exception {
        String uri = "http://soundcloud.com/unsupported-by-android";
        ApiResolvedResource resolvedResource = unsupportedResource();
        mockResolutionFor(uri, resolvedResource);

        operations.resolve(uri).test()
                  .assertValue(ResolveResult.error(Uri.parse(uri), null))
                  .assertComplete();
    }

    @Test
    public void shouldReportExceptionWithError() throws Exception {
        String uri = "http://soundcloud.com/exceptiontime";
        when(apiClient.mappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.RESOLVE_ENTITY.path()).withQueryParam("identifier", uri)),
                eq(ApiResolvedResource.class))).thenReturn(Single.error(new IOException()));

        ResolveResult resolveResult = operations.resolve(uri).test()
                                                .assertValueCount(1)
                                                .assertComplete()
                                                .values().get(0);
        assertThat(resolveResult.success()).isFalse();
        assertThat(resolveResult.exception().get()).isInstanceOf(IOException.class);
    }

    @Test
    public void followsClickTrackingUrlsThenResolvesUrlQuery() throws Exception {
        String uri =
                "http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music";
        ApiResolvedResource resolvedResource = resolvedTrack();
        when(apiClient.ignoreResultRequest(any(ApiRequest.class))).thenReturn(Completable.complete());
        mockResolutionFor("http://soundcloud.com/skrillex/music", resolvedResource);

        operations.resolve(uri).test()
                  .assertValue(ResolveResult.succes(resolvedResource.getOptionalTrack().get().getUrn()))
                  .assertComplete();
    }

    @Test
    public void failsFollowClickTrackingUrlsThenResolvesUrlQuery() throws Exception {
        String uri =
                "http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music";
        ApiResolvedResource resolvedResource = resolvedTrack();
        when(apiClient.ignoreResultRequest(any(ApiRequest.class))).thenReturn(Completable.error(new IOException()));
        mockResolutionFor("http://soundcloud.com/skrillex/music", resolvedResource);

        operations.resolve(uri).test()
                  .assertValue(ResolveResult.succes(resolvedResource.getOptionalTrack().get().getUrn()))
                  .assertComplete();
    }

    @Test
    public void failsFollowClickTrackingUrlsThenFailsResolveUrlQuery() throws Exception {
        String uri =
                "http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music";
        ApiResolvedResource resolvedResource = resolvedTrack();
        IOException exception = new IOException();
        when(apiClient.ignoreResultRequest(any(ApiRequest.class))).thenReturn(Completable.error(exception));
        String followUrl = "http://soundcloud.com/skrillex/music";
        when(apiClient.mappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.RESOLVE_ENTITY.path()).withQueryParam("identifier", followUrl)),
                eq(ApiResolvedResource.class))).thenReturn(Single.error(exception));

        ResolveResult resolveResult = operations.resolve(uri).test()
                                                .assertComplete()
                                                .assertValueCount(1)
                                                .values()
                                                .get(0);
        assertThat(resolveResult.success()).isFalse();
        assertThat(resolveResult.uri().orNull()).isEqualTo(Uri.parse(followUrl));
    }

    @Test
    public void storesTrack() throws Exception {
        String uri = "http://soundcloud.com/skrillex/ye";
        ApiResolvedResource resolvedResource = resolvedTrack();
        mockResolutionFor(uri, resolvedResource);

        operations.resolve(uri).test();

        verify(storeTracksCommand).call(Collections.singletonList(resolvedResource.getOptionalTrack().get()));
    }

    @Test
    public void storesPlaylist() throws Exception {
        String uri = "http://soundcloud.com/skrillex/sets/yo";
        ApiResolvedResource resolvedResource = resolvedPlaylist();
        mockResolutionFor(uri, resolvedResource);

        operations.resolve(uri).test();

        verify(storePlaylistsCommand).call(Collections.singletonList(resolvedResource.getOptionalPlaylist().get()));
    }

    @Test
    public void storesUser() throws Exception {
        String uri = "http://soundcloud.com/skrillex";
        ApiResolvedResource resolvedResource = resolvedUser();
        mockResolutionFor(uri, resolvedResource);

        operations.resolve(uri).test();

        verify(storeUsersCommand).call(Collections.singletonList(resolvedResource.getOptionalUser().get()));
    }

    @Test
    public void storesStation() throws Exception {
        String uri = "http://soundcloud.com/stations/artist/tycho";
        ApiResolvedResource resolvedResource = resolvedStation();
        mockResolutionFor(uri, resolvedResource);

        operations.resolve(uri).test();

        verify(storeStationsCommand).call(resolvedResource.getOptionalStation().get());
    }

    private ApiResolvedResource resolvedTrack() {
        return new ApiResolvedResource(ModelFixtures.create(ApiTrack.class), null, null, null);
    }

    private ApiResolvedResource resolvedPlaylist() {
        return new ApiResolvedResource(null, ModelFixtures.create(ApiPlaylist.class), null, null);
    }

    private ApiResolvedResource resolvedUser() {
        return new ApiResolvedResource(null, null, ModelFixtures.create(ApiUser.class), null);
    }

    private ApiResolvedResource resolvedStation() {
        return new ApiResolvedResource(null, null, null, StationFixtures.getApiStation());
    }

    private ApiResolvedResource unsupportedResource() {
        return new ApiResolvedResource(null, null, null, null);
    }

    private void mockResolutionFor(String url, ApiResolvedResource resolvedResource) throws Exception {
        when(apiClient.mappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.RESOLVE_ENTITY.path()).withQueryParam("identifier", url)),
                eq(ApiResolvedResource.class))).thenReturn(Single.just(resolvedResource));
    }
}
