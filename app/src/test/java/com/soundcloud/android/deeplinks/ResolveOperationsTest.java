package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import android.net.Uri;

import java.io.IOException;
import java.util.Collections;

public class ResolveOperationsTest extends AndroidUnitTest {

    private ResolveOperations operations;

    @Mock private ApiClient apiClient;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;

    private Scheduler scheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        operations = new ResolveOperations(apiClient, scheduler,
                                           storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void fetchesResourceViaResolveEndpoint() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex");
        ApiResolvedResource resolvedResource = resolvedTrack();
        mockResolutionFor(uri.toString(), resolvedResource);

        operations.resolve(uri).test()
                .assertValue(ResolveResult.succes(resolvedResource.getOptionalTrack().get().getUrn()))
                .assertCompleted();
    }

    @Test
    public void shouldReportUnsupportedResourcesAsError() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/unsupported-by-android");
        ApiResolvedResource resolvedResource = unsupportedResource();
        mockResolutionFor(uri.toString(), resolvedResource);

        operations.resolve(uri).test()
                  .assertValue(ResolveResult.error(uri, null))
                  .assertCompleted();
    }

    @Test
    public void shouldReportExceptionWithError() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/exceptiontime");
        final IOException ioException = new IOException();
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.RESOLVE_ENTITY.path()).withQueryParam("identifier", uri.toString())),
                eq(ApiResolvedResource.class))).thenThrow(ioException);

        operations.resolve(uri).test()
                .assertValue(ResolveResult.error(uri, ioException))
                .assertCompleted();
    }

    @Test
    public void followsClickTrackingUrlsThenResolvesUrlQuery() throws Exception {
        Uri uri = Uri.parse(
                "http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music");
        ApiResolvedResource resolvedResource = resolvedTrack();
        mockResolutionFor("http://soundcloud.com/skrillex/music", resolvedResource);

        operations.resolve(uri).test()
                  .assertValue(ResolveResult.succes(resolvedResource.getOptionalTrack().get().getUrn()))
                  .assertCompleted();
    }

    @Test
    public void storesTrack() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex/ye");
        ApiResolvedResource resolvedResource = resolvedTrack();
        mockResolutionFor(uri.toString(), resolvedResource);

        operations.resolve(uri).test();

        verify(storeTracksCommand).call(Collections.singletonList(resolvedResource.getOptionalTrack().get()));
    }

    @Test
    public void storesPlaylist() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex/sets/yo");
        ApiResolvedResource resolvedResource = resolvedPlaylist();
        mockResolutionFor(uri.toString(), resolvedResource);

        operations.resolve(uri).test();

        verify(storePlaylistsCommand).call(Collections.singletonList(resolvedResource.getOptionalPlaylist().get()));
    }

    @Test
    public void storesUser() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex");
        ApiResolvedResource resolvedResource = resolvedUser();
        mockResolutionFor(uri.toString(), resolvedResource);

        operations.resolve(uri).test();

        verify(storeUsersCommand).call(Collections.singletonList(resolvedResource.getOptionalUser().get()));
    }

    private ApiResolvedResource resolvedTrack() {
        return new ApiResolvedResource(ModelFixtures.create(ApiTrack.class), null, null);
    }

    private ApiResolvedResource resolvedPlaylist() {
        return new ApiResolvedResource(null, ModelFixtures.create(ApiPlaylist.class), null);
    }

    private ApiResolvedResource resolvedUser() {
        return new ApiResolvedResource(null, null, ModelFixtures.create(ApiUser.class));
    }

    private ApiResolvedResource unsupportedResource() {
        return new ApiResolvedResource(null, null, null);
    }

    private void mockResolutionFor(String url, ApiResolvedResource resolvedResource) throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.RESOLVE_ENTITY.path()).withQueryParam("identifier", url)),
                eq(ApiResolvedResource.class))).thenReturn(resolvedResource);
    }
}
