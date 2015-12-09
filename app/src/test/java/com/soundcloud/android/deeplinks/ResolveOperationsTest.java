package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observer;
import rx.Scheduler;
import rx.exceptions.OnErrorThrowable;
import rx.schedulers.Schedulers;

import android.net.Uri;

import java.util.Collections;

public class ResolveOperationsTest extends AndroidUnitTest {

    private ResolveOperations operations;

    @Mock private Observer<Urn> observer;
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

        operations.resolve(uri).subscribe(observer);

        verify(observer).onNext(resolvedResource.getOptionalTrack().get().getUrn());
        verify(observer).onCompleted();
    }

    @Test
    public void shouldReportUnsupportedResourcesAsError() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/unsupported-by-android");
        ApiResolvedResource resolvedResource = unsupportedResource();
        mockResolutionFor(uri.toString(), resolvedResource);

        operations.resolve(uri).subscribe(observer);

        ArgumentCaptor<OnErrorThrowable.OnNextValue> captor = ArgumentCaptor.forClass(OnErrorThrowable.OnNextValue.class);
        verify(observer).onError(captor.capture());
        assertThat(captor.getValue().getValue()).isEqualTo(uri);
    }

    @Test
    public void followsClickTrackingUrlsThenResolvesUrlQuery() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music");
        ApiResolvedResource resolvedResource = resolvedTrack();
        mockResolutionFor("http://soundcloud.com/skrillex/music", resolvedResource);

        operations.resolve(uri).subscribe(observer);

        verify(observer).onNext(resolvedResource.getOptionalTrack().get().getUrn());
        verify(observer).onCompleted();
    }

    @Test
    public void storesTrack() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex/ye");
        ApiResolvedResource resolvedResource = resolvedTrack();
        mockResolutionFor(uri.toString(), resolvedResource);

        operations.resolve(uri).subscribe(observer);

        verify(storeTracksCommand).call(Collections.singletonList(resolvedResource.getOptionalTrack().get()));
    }

    @Test
    public void storesPlaylist() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex/sets/yo");
        ApiResolvedResource resolvedResource = resolvedPlaylist();
        mockResolutionFor(uri.toString(), resolvedResource);

        operations.resolve(uri).subscribe(observer);

        verify(storePlaylistsCommand).call(Collections.singletonList(resolvedResource.getOptionalPlaylist().get()));
    }

    @Test
    public void storesUser() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex");
        ApiResolvedResource resolvedResource = resolvedUser();
        mockResolutionFor(uri.toString(), resolvedResource);

        operations.resolve(uri).subscribe(observer);

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
