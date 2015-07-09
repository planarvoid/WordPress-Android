package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observer;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import android.net.Uri;

import java.util.Collections;

public class ResolveOperationsTest extends AndroidUnitTest {

    private ResolveOperations operations;

    @Mock private Observer<PublicApiResource> observer;
    @Mock private ApiClient apiClient;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;

    private Scheduler scheduler = Schedulers.immediate();
    private PublicApiResource resource;

    @Before
    public void setUp() throws Exception {
        operations = new ResolveOperations(apiClient, scheduler,
                storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void fetchesResourceViaResolveEndpoint() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex");
        setupResourceForUrl(uri.toString());

        operations.resolve(uri).subscribe(observer);

        verify(observer).onNext(resource);
        verify(observer).onCompleted();
    }

    @Test
    public void fetchesResourceUsingUrnResolverViaResolveEndpoint() throws Exception {
        Uri uri = Uri.parse("soundcloud://sounds:12345");
        setupResourceForUrl("soundcloud:tracks:12345");

        operations.resolve(uri).subscribe(observer);

        verify(observer).onNext(resource);
        verify(observer).onCompleted();
    }

    @Test
    public void followsClickTrackingUrlsThenResolvesUrlQuery() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music");
        setupResourceForUrl("http://soundcloud.com/skrillex/music");

        operations.resolve(uri).subscribe(observer);

        verify(observer).onNext(resource);
        verify(observer).onCompleted();
    }

    @Test
    public void storesTrack() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex/ye");
        setupResourceForUrl(uri.toString(), PublicApiTrack.class);

        operations.resolve(uri).subscribe(observer);

        verify(storeTracksCommand).call(Collections.singletonList(((PublicApiTrack) resource).toApiMobileTrack()));
    }

    @Test
    public void storesPlaylist() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex/sets/yo");
        setupResourceForUrl(uri.toString(), PublicApiPlaylist.class);

        operations.resolve(uri).subscribe(observer);

        verify(storePlaylistsCommand).call(Collections.singletonList(((PublicApiPlaylist) resource).toApiMobilePlaylist()));
    }

    @Test
    public void storesUser() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex");
        setupResourceForUrl(uri.toString(), PublicApiUser.class);

        operations.resolve(uri).subscribe(observer);

        verify(storeUsersCommand).call(Collections.singletonList(((PublicApiUser) resource).toApiMobileUser()));
    }

    private void setupResourceForUrl(String url) throws Exception {
        setupResourceForUrl(url, PublicApiPlaylist.class);
    }

    private void setupResourceForUrl(String url, Class resourceClass) throws Exception {
        resource = (PublicApiResource) ModelFixtures.create(resourceClass);

        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("GET", ApiEndpoints.RESOLVE.path()).withQueryParam("url", url)),
                eq(PublicApiResource.class))).thenReturn(resource);
    }
}
