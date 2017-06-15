package com.soundcloud.android.navigation;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationsStorage;
import com.soundcloud.android.stations.StoreStationCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackStorage;
import com.soundcloud.android.users.UserStorage;
import io.reactivex.Completable;
import io.reactivex.Maybe;
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
    @Mock private TrackStorage trackStorage;
    @Mock private PlaylistStorage playlistStorage;
    @Mock private UserStorage userStorage;
    @Mock private StationsStorage stationsStorage;

    private Scheduler scheduler = Schedulers.trampoline();

    @Before
    public void setUp() throws Exception {
        operations = new ResolveOperations(apiClient,
                                           scheduler,
                                           storeTracksCommand,
                                           storePlaylistsCommand,
                                           storeUsersCommand,
                                           storeStationsCommand,
                                           trackStorage,
                                           playlistStorage,
                                           userStorage,
                                           stationsStorage);

        when(trackStorage.urnForPermalink(anyString())).thenReturn(Maybe.empty());
        when(userStorage.urnForPermalink(anyString())).thenReturn(Maybe.empty());
        when(playlistStorage.urnForPermalink(anyString())).thenReturn(Maybe.empty());
        when(stationsStorage.urnForPermalink(anyString())).thenReturn(Maybe.empty());
    }

    @Test
    public void fetchesResourceViaResolveEndpoint() throws Exception {
        String uri = "http://soundcloud.com/skrillex";
        ApiResolvedResource resolvedResource = resolvedTrack();
        mockResolutionFor(uri, resolvedResource);

        operations.resolve(uri).test()
                  .assertValue(ResolveResult.success(resolvedResource.getOptionalTrack().get().getUrn()))
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
    public void followsClickTrackingUrlsThenResolvesUrlQueryViaApi() throws Exception {
        String uri =
                "http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music";
        ApiResolvedResource resolvedResource = resolvedTrack();
        when(apiClient.ignoreResultRequest(any(ApiRequest.class))).thenReturn(Completable.complete());
        mockResolutionFor("http://soundcloud.com/skrillex/music", resolvedResource);

        operations.resolve(uri).test()
                  .assertValue(ResolveResult.success(resolvedResource.getOptionalTrack().get().getUrn()))
                  .assertComplete();
    }

    @Test
    public void followsClickTrackingUrlsThenResolvesUrlQueryLocal() throws Exception {
        String uri =
                "http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music";
        ApiResolvedResource resolvedResource = resolvedTrack();
        when(apiClient.ignoreResultRequest(any(ApiRequest.class))).thenReturn(Completable.complete());
        when(trackStorage.urnForPermalink("skrillex/music")).thenReturn(Maybe.just(resolvedResource.getOptionalTrack().get().getUrn()));

        operations.resolve(uri).test()
                .assertValue(ResolveResult.success(resolvedResource.getOptionalTrack().get().getUrn()))
                .assertComplete();
    }

    @Test
    public void failsToFollowClickTrackingUrlsThenResolvesUrlQueryViaApi() throws Exception {
        String uri =
                "http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music";
        ApiResolvedResource resolvedResource = resolvedTrack();
        when(apiClient.ignoreResultRequest(any(ApiRequest.class))).thenReturn(Completable.error(new IOException()));
        mockResolutionFor("http://soundcloud.com/skrillex/music", resolvedResource);

        operations.resolve(uri).test()
                  .assertValue(ResolveResult.success(resolvedResource.getOptionalTrack().get().getUrn()))
                  .assertComplete();
    }

    @Test
    public void failsToFollowClickTrackingUrlsThenResolvesUrlQueryLocal() throws Exception {
        String uri =
                "http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music";
        ApiResolvedResource resolvedResource = resolvedTrack();
        when(apiClient.ignoreResultRequest(any(ApiRequest.class))).thenReturn(Completable.error(new IOException()));
        when(trackStorage.urnForPermalink("skrillex/music")).thenReturn(Maybe.just(resolvedResource.getOptionalTrack().get().getUrn()));

        operations.resolve(uri).test()
                .assertValue(ResolveResult.success(resolvedResource.getOptionalTrack().get().getUrn()))
                .assertComplete();
    }

    @Test
    public void failsToFollowClickTrackingUrlsThenFailsResolveUrlQuery() throws Exception {
        String uri =
                "http://soundcloud.com/-/t/click/best-email?url=http%3A%2F%2Fsoundcloud.com%2Fskrillex/music";
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
    public void resolveTrackLocal() throws Exception {
        String uri = "http://soundcloud.com/skrillex/ye";
        ApiResolvedResource resolvedResource = resolvedTrack();
        when(trackStorage.urnForPermalink("skrillex/ye")).thenReturn(Maybe.just(resolvedResource.getOptionalTrack().get().getUrn()));

        operations.resolve(uri).test()
                .assertValue(ResolveResult.success(resolvedResource.getOptionalTrack().get().getUrn()))
                .assertComplete();

        verifyZeroInteractions(storePlaylistsCommand, storeStationsCommand, storeTracksCommand, storeUsersCommand);
    }

    @Test
    public void resolveUserLocal() throws Exception {
        String uri = "http://soundcloud.com/skrillex";
        ApiResolvedResource resolvedResource = resolvedUser();

        when(userStorage.urnForPermalink("skrillex")).thenReturn(Maybe.just(resolvedResource.getOptionalUser().get().getUrn()));

        operations.resolve(uri).test()
                .assertValue(ResolveResult.success(resolvedResource.getOptionalUser().get().getUrn()))
                .assertComplete();

        verifyZeroInteractions(storePlaylistsCommand, storeStationsCommand, storeTracksCommand, storeUsersCommand);
    }

    @Test
    public void resolvePlaylistLocal() throws Exception {
        String uri = "http://soundcloud.com/skrillex/sets/yo";
        ApiResolvedResource resolvedResource = resolvedPlaylist();

        when(playlistStorage.urnForPermalink("skrillex/sets/yo")).thenReturn(Maybe.just(resolvedResource.getOptionalPlaylist().get().getUrn()));

        operations.resolve(uri).test()
                .assertValue(ResolveResult.success(resolvedResource.getOptionalPlaylist().get().getUrn()))
                .assertComplete();

        verifyZeroInteractions(storePlaylistsCommand, storeStationsCommand, storeTracksCommand, storeUsersCommand);
    }

    @Test
    public void resolveArtistStationLocal() throws Exception {
        String uri = "http://soundcloud.com/stations/artist/tycho";
        ApiResolvedResource resolvedResource = resolvedStation();

        when(stationsStorage.urnForPermalink("stations/artist/tycho")).thenReturn(Maybe.just(resolvedResource.getOptionalStation().get().getUrn()));

        operations.resolve(uri).test()
                .assertValue(ResolveResult.success(resolvedResource.getOptionalStation().get().getUrn()))
                .assertComplete();

        verifyZeroInteractions(storePlaylistsCommand, storeStationsCommand, storeTracksCommand, storeUsersCommand);
    }

    @Test
    public void resolveTrackStationLocal() throws Exception {
        String uri = "http://soundcloud.com/stations/track/tycho";
        ApiResolvedResource resolvedResource = resolvedStation();

        when(stationsStorage.urnForPermalink("stations/track/tycho")).thenReturn(Maybe.just(resolvedResource.getOptionalStation().get().getUrn()));

        operations.resolve(uri).test()
                .assertValue(ResolveResult.success(resolvedResource.getOptionalStation().get().getUrn()))
                .assertComplete();

        verifyZeroInteractions(storePlaylistsCommand, storeStationsCommand, storeTracksCommand, storeUsersCommand);
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

    @Test
    public void extractPermalinkHttps() throws Exception {
        assertThat(operations.extractPermalink("https://soundcloud.com/tycho")).isEqualTo("tycho");
        assertThat(operations.extractPermalink("https://soundcloud.com/tycho/tracksample")).isEqualTo("tycho/tracksample");
        assertThat(operations.extractPermalink("https://soundcloud.com/tycho/sets/sample")).isEqualTo("tycho/sets/sample");
        assertThat(operations.extractPermalink("https://soundcloud.com/stations/artist/tycho")).isEqualTo("stations/artist/tycho");
        assertThat(operations.extractPermalink("https://soundcloud.com/stations/track/tycho/edc-sunrise")).isEqualTo("stations/track/tycho/edc-sunrise");
    }

    @Test
    public void extractPermalinkHttp() throws Exception {
        assertThat(operations.extractPermalink("http://soundcloud.com/tycho")).isEqualTo("tycho");
        assertThat(operations.extractPermalink("http://soundcloud.com/tycho/tracksample")).isEqualTo("tycho/tracksample");
        assertThat(operations.extractPermalink("http://soundcloud.com/tycho/sets/sample")).isEqualTo("tycho/sets/sample");
        assertThat(operations.extractPermalink("http://soundcloud.com/stations/artist/tycho")).isEqualTo("stations/artist/tycho");
        assertThat(operations.extractPermalink("http://soundcloud.com/stations/track/tycho/edc-sunrise")).isEqualTo("stations/track/tycho/edc-sunrise");
    }

    @Test
    public void extractPermalinkMobileHttps() throws Exception {
        assertThat(operations.extractPermalink("https://m.soundcloud.com/tycho")).isEqualTo("tycho");
        assertThat(operations.extractPermalink("https://m.soundcloud.com/tycho/tracksample")).isEqualTo("tycho/tracksample");
        assertThat(operations.extractPermalink("https://m.soundcloud.com/tycho/sets/sample")).isEqualTo("tycho/sets/sample");
        assertThat(operations.extractPermalink("https://m.soundcloud.com/stations/artist/tycho")).isEqualTo("stations/artist/tycho");
        assertThat(operations.extractPermalink("https://m.soundcloud.com/stations/track/tycho/edc-sunrise")).isEqualTo("stations/track/tycho/edc-sunrise");
    }

    @Test
    public void extractPermalinkMobileHttp() throws Exception {
        assertThat(operations.extractPermalink("http://m.soundcloud.com/tycho")).isEqualTo("tycho");
        assertThat(operations.extractPermalink("http://m.soundcloud.com/tycho/tracksample")).isEqualTo("tycho/tracksample");
        assertThat(operations.extractPermalink("http://m.soundcloud.com/tycho/sets/sample")).isEqualTo("tycho/sets/sample");
        assertThat(operations.extractPermalink("http://m.soundcloud.com/stations/artist/tycho")).isEqualTo("stations/artist/tycho");
        assertThat(operations.extractPermalink("http://m.soundcloud.com/stations/track/tycho/edc-sunrise")).isEqualTo("stations/track/tycho/edc-sunrise");
    }

    @Test
    public void extractPermalinkWWWHttps() throws Exception {
        assertThat(operations.extractPermalink("https://www.soundcloud.com/tycho")).isEqualTo("tycho");
        assertThat(operations.extractPermalink("https://www.soundcloud.com/tycho/tracksample")).isEqualTo("tycho/tracksample");
        assertThat(operations.extractPermalink("https://www.soundcloud.com/tycho/sets/sample")).isEqualTo("tycho/sets/sample");
        assertThat(operations.extractPermalink("https://www.soundcloud.com/stations/artist/tycho")).isEqualTo("stations/artist/tycho");
        assertThat(operations.extractPermalink("https://www.soundcloud.com/stations/track/tycho/edc-sunrise")).isEqualTo("stations/track/tycho/edc-sunrise");
    }

    @Test
    public void extractPermalinkWWWHttp() throws Exception {
        assertThat(operations.extractPermalink("http://www.soundcloud.com/tycho")).isEqualTo("tycho");
        assertThat(operations.extractPermalink("http://www.soundcloud.com/tycho/tracksample")).isEqualTo("tycho/tracksample");
        assertThat(operations.extractPermalink("http://www.soundcloud.com/tycho/sets/sample")).isEqualTo("tycho/sets/sample");
        assertThat(operations.extractPermalink("http://www.soundcloud.com/stations/artist/tycho")).isEqualTo("stations/artist/tycho");
        assertThat(operations.extractPermalink("http://www.soundcloud.com/stations/track/tycho/edc-sunrise")).isEqualTo("stations/track/tycho/edc-sunrise");
    }



    @Test
    public void extractPermalinkWWWMobileHttps() throws Exception {
        assertThat(operations.extractPermalink("https://www.m.soundcloud.com/tycho")).isEqualTo("tycho");
        assertThat(operations.extractPermalink("https://www.m.soundcloud.com/tycho/tracksample")).isEqualTo("tycho/tracksample");
        assertThat(operations.extractPermalink("https://www.m.soundcloud.com/tycho/sets/sample")).isEqualTo("tycho/sets/sample");
        assertThat(operations.extractPermalink("https://www.m.soundcloud.com/stations/artist/tycho")).isEqualTo("stations/artist/tycho");
        assertThat(operations.extractPermalink("https://www.m.soundcloud.com/stations/track/tycho/edc-sunrise")).isEqualTo("stations/track/tycho/edc-sunrise");
    }

    @Test
    public void extractPermalinkWWWMobileHttp() throws Exception {
        assertThat(operations.extractPermalink("http://www.m.soundcloud.com/tycho")).isEqualTo("tycho");
        assertThat(operations.extractPermalink("http://www.m.soundcloud.com/tycho/tracksample")).isEqualTo("tycho/tracksample");
        assertThat(operations.extractPermalink("http://www.m.soundcloud.com/tycho/sets/sample")).isEqualTo("tycho/sets/sample");
        assertThat(operations.extractPermalink("http://www.m.soundcloud.com/stations/artist/tycho")).isEqualTo("stations/artist/tycho");
        assertThat(operations.extractPermalink("http://www.m.soundcloud.com/stations/track/tycho/edc-sunrise")).isEqualTo("stations/track/tycho/edc-sunrise");
    }

    @Test
    public void extractPermalinkSoundcloudSchema() throws Exception {
        assertThat(operations.extractPermalink("soundcloud://tycho")).isEqualTo("tycho");
        assertThat(operations.extractPermalink("soundcloud://tycho/tracksample")).isEqualTo("tycho/tracksample");
        assertThat(operations.extractPermalink("soundcloud://tycho/sets/sample")).isEqualTo("tycho/sets/sample");
        assertThat(operations.extractPermalink("soundcloud://stations/artist/tycho")).isEqualTo("stations/artist/tycho");
        assertThat(operations.extractPermalink("soundcloud://stations/track/tycho/edc-sunrise")).isEqualTo("stations/track/tycho/edc-sunrise");
    }

    @Test
    public void isStationsPermalink() throws Exception {
        assertThat(operations.isStationsPermalink("tycho")).isFalse();
        assertThat(operations.isStationsPermalink("tycho/tracksample")).isFalse();
        assertThat(operations.isStationsPermalink("tycho/sets/sample")).isFalse();
        assertThat(operations.isStationsPermalink("stations/artist/tycho")).isTrue();
        assertThat(operations.isStationsPermalink("stations/track/tycho/edc-sunrise")).isTrue();
        assertThat(operations.isStationsPermalink("stations/testing/tycho/edc-sunrise")).isFalse();
    }

    @Test
    public void isUserPermalink() throws Exception {
        assertThat(operations.isUserPermalink("tycho")).isTrue();
        assertThat(operations.isUserPermalink("tycho/tracksample")).isFalse();
        assertThat(operations.isUserPermalink("tycho/sets/sample")).isFalse();
        assertThat(operations.isUserPermalink("stations/artist/tycho")).isFalse();
        assertThat(operations.isUserPermalink("stations/track/tycho/edc-sunrise")).isFalse();
        assertThat(operations.isUserPermalink("stations/testing/tycho/edc-sunrise")).isFalse();
    }

    @Test
    public void isPlaylistPermalink() throws Exception {
        assertThat(operations.isPlaylistPermalink("tycho")).isFalse();
        assertThat(operations.isPlaylistPermalink("tycho/tracksample")).isFalse();
        assertThat(operations.isPlaylistPermalink("tycho/sets/sample")).isTrue();
        assertThat(operations.isPlaylistPermalink("stations/artist/tycho")).isFalse();
        assertThat(operations.isPlaylistPermalink("stations/track/tycho/edc-sunrise")).isFalse();
        assertThat(operations.isPlaylistPermalink("stations/testing/tycho/edc-sunrise")).isFalse();
    }

    @Test
    public void isTrackPermalink() throws Exception {
        assertThat(operations.isTrackPermalink("tycho")).isFalse();
        assertThat(operations.isTrackPermalink("tycho/tracksample")).isTrue();
        assertThat(operations.isTrackPermalink("tycho/sets/sample")).isFalse();
        assertThat(operations.isTrackPermalink("stations/artist/tycho")).isFalse();
        assertThat(operations.isTrackPermalink("stations/track/tycho/edc-sunrise")).isFalse();
        assertThat(operations.isTrackPermalink("stations/testing/tycho/edc-sunrise")).isFalse();
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
