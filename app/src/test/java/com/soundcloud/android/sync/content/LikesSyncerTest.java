package com.soundcloud.android.sync.content;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestMethod;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static com.soundcloud.android.utils.CollectionUtils.toPropertySets;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.likes.ApiLike;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.likes.LikeStorage;
import com.soundcloud.android.likes.LikesWriteStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.ApiPlaylistCollection;
import com.soundcloud.android.playlists.PlaylistWriteStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestApiResponses;
import com.soundcloud.android.tracks.ApiTrackCollection;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.verification.VerificationMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

@RunWith(SoundCloudTestRunner.class)
public class LikesSyncerTest {

    private LikesSyncer syncer;

    private final Urn userUrn = new Urn("soundcloud:users:123");
    private ApiLike trackLike;
    private ApiLike playlistLike;

    @Mock private ApiClient apiClient;
    @Mock private LikeStorage likesStorage;
    @Mock private LikesWriteStorage likesWriteStorage;
    @Mock private TrackWriteStorage trackWriteStorage;
    @Mock private PlaylistWriteStorage playlistWriteStorage;
    @Mock private AccountOperations accountOperations;

    @Before
    public void setup() throws Exception {
        syncer = new LikesSyncer(apiClient, likesStorage, likesWriteStorage, trackWriteStorage,
                playlistWriteStorage, accountOperations);
        trackLike = ModelFixtures.apiTrackLike();
        playlistLike = ModelFixtures.apiPlaylistLike();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
    }

    @Test
    public void shouldDoNothingIfLocalAndRemoteStateAreIdentical() throws Exception {
        withRemoteTrackLikes(trackLike);
        withRemotePlaylistLikes(playlistLike);
        withLocalTrackLikes(trackLike);
        withLocalPlaylistLikes(playlistLike);

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.UNCHANGED);

        verify(likesWriteStorage, never()).removeLikes(anyCollection());
        verify(likesWriteStorage, never()).storeLikes(anyCollection());
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
        verifyRemotePlaylistLikeAddition(never(), playlistLike);
        verifyRemotePlaylistLikeRemoval(never(), playlistLike);
    }

    @Test
    public void shouldCreateLikeRemotelyIfExistsLocallyButNotRemotely() throws Exception {
        withRemoteTrackLikes();
        withRemotePlaylistLikes();
        withLocalTrackLikes(trackLike);
        withLocalPlaylistLikes(playlistLike);

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.UNCHANGED);

        verifyRemoteTrackLikeAddition(times(1), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
        verifyRemotePlaylistLikeAddition(times(1), playlistLike);
        verifyRemotePlaylistLikeRemoval(never(), playlistLike);
        verify(likesWriteStorage, never()).removeLikes(anyCollection());
        verify(likesWriteStorage, never()).storeLikes(anyCollection());
    }

    @Test
    public void shouldCreateLikeLocallyIfExistsRemotelyButNotLocally() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class))).thenReturn(new ApiTrackCollection());
        withRemoteTrackLikes(trackLike);
        withRemotePlaylistLikes(playlistLike);
        withLocalTrackLikes();
        withLocalPlaylistLikes();

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        verify(likesWriteStorage).storeLikes(new HashSet(Arrays.asList(trackLike.toPropertySet())));
        verify(likesWriteStorage).storeLikes(new HashSet(Arrays.asList(playlistLike.toPropertySet())));
        verify(likesWriteStorage, never()).removeLikes(anyCollection());
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
        verifyRemotePlaylistLikeAddition(never(), playlistLike);
        verifyRemotePlaylistLikeRemoval(never(), playlistLike);
    }

    @Test
    public void shouldRemoveLikeRemotelyIfLocalLikeIsPendingRemovalAndExistsRemotelyWithOlderTimestamp() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));
        PropertySet playlistLikePendingRemoval = playlistLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(playlistLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes(trackLike);
        withRemotePlaylistLikes(playlistLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        withLocalPlaylistLikes();
        withLocalPlaylistLikesPendingRemoval(playlistLikePendingRemoval);
        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(TestApiResponses.ok());

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        verifyRemoteTrackLikeRemoval(times(1), trackLike);
        verifyRemotePlaylistLikeRemoval(times(1), playlistLike);
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemotePlaylistLikeAddition(never(), playlistLike);
    }

    @Test
    public void shouldRemoveLikeLocallyIfPendingRemovalRequestSucceeded() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));
        PropertySet playlistLikePendingRemoval = playlistLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(playlistLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes(trackLike);
        withRemotePlaylistLikes(playlistLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        withLocalPlaylistLikes();
        withLocalPlaylistLikesPendingRemoval(playlistLikePendingRemoval);
        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(TestApiResponses.ok());

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        verify(likesWriteStorage).removeLikes(new HashSet(Arrays.asList(trackLikePendingRemoval)));
        verify(likesWriteStorage).removeLikes(new HashSet(Arrays.asList(playlistLikePendingRemoval)));
        verify(likesWriteStorage, never()).storeLikes(anyCollection());
    }

    @Test
    public void shouldNotRemoveLikeLocallyIfPendingRemovalRequestFailed() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));
        PropertySet playlistLikePendingRemoval = playlistLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(playlistLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes(trackLike);
        withRemotePlaylistLikes(playlistLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        withLocalPlaylistLikes();
        withLocalPlaylistLikesPendingRemoval(playlistLikePendingRemoval);
        when(apiClient.fetchResponse(argThat(isPublicApiRequestMethod("DELETE")))).thenReturn(TestApiResponses.status(500), TestApiResponses.ok());

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        // only remove the playlist like
        verify(likesWriteStorage).removeLikes(new HashSet(Arrays.asList(playlistLikePendingRemoval)));
        verify(likesWriteStorage, never()).removeLikes(new HashSet(Arrays.asList(trackLikePendingRemoval)));
        verify(likesWriteStorage, never()).storeLikes(anyCollection());
    }

    @Test
    public void shouldRemoveLikeOnlyLocallyIfPendingRemovalAndDoesNotExistRemotely() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));
        PropertySet playlistLikePendingRemoval = playlistLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(playlistLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes();
        withRemotePlaylistLikes();
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        withLocalPlaylistLikes();
        withLocalPlaylistLikesPendingRemoval(playlistLikePendingRemoval);

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        verify(likesWriteStorage).removeLikes(new HashSet(Arrays.asList(trackLikePendingRemoval)));
        verify(likesWriteStorage).removeLikes(new HashSet(Arrays.asList(playlistLikePendingRemoval)));
        verify(likesWriteStorage, never()).storeLikes(anyCollection());
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
        verifyRemotePlaylistLikeAddition(never(), playlistLike);
        verifyRemotePlaylistLikeRemoval(never(), playlistLike);
    }

    @Test
    public void shouldReplaceLocalLikePendingRemovalIfRemoteLikeExistsAndHasNewerTimestamp() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class))).thenReturn(new ApiTrackCollection());
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() - 1));
        PropertySet playlistLikePendingRemoval = playlistLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(playlistLike.getCreatedAt().getTime() - 1));

        withRemoteTrackLikes(trackLike);
        withRemotePlaylistLikes(playlistLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        withLocalPlaylistLikes();
        withLocalPlaylistLikesPendingRemoval(playlistLikePendingRemoval);

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        verify(likesWriteStorage).storeLikes(new HashSet(toPropertySets(Arrays.asList(trackLike))));
        verify(likesWriteStorage).storeLikes(new HashSet(toPropertySets(Arrays.asList(playlistLike))));
        verify(likesWriteStorage, never()).removeLikes(anyCollection());
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
        verifyRemotePlaylistLikeAddition(never(), playlistLike);
        verifyRemotePlaylistLikeRemoval(never(), playlistLike);
    }

    @Test
    public void mixedScenario() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class))).thenReturn(new ApiTrackCollection());
        // remote
        ApiLike existsRemotelyNotLocally = ModelFixtures.apiTrackLike();
        ApiLike existsRemotelyPendingRemoval = ModelFixtures.apiTrackLike();
        // local
        ApiLike existsLocallyNotRemotely = ModelFixtures.apiTrackLike();
        PropertySet existsLocallyPendingRemoval = existsRemotelyPendingRemoval.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(existsRemotelyPendingRemoval.getCreatedAt().getTime() + 1));
        PropertySet existsLocallyNotRemotelyPendingRemoval =
                ModelFixtures.apiTrackLike().toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date());
        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(TestApiResponses.ok());

        // expected outcome:
        // - one local addition
        // - two local removals
        // - one remote addition
        // - one remote removal
        withRemoteTrackLikes(existsRemotelyNotLocally, existsRemotelyPendingRemoval);
        withLocalTrackLikes(existsLocallyNotRemotely.toPropertySet());
        withLocalTrackLikesPendingRemoval(existsLocallyPendingRemoval, existsLocallyNotRemotelyPendingRemoval);
        withLocalPlaylistLikes();
        withRemotePlaylistLikes();

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        verify(likesWriteStorage).storeLikes(new HashSet(toPropertySets(Arrays.asList(existsRemotelyNotLocally))));
        verify(likesWriteStorage).removeLikes(new HashSet(Arrays.asList(existsLocallyPendingRemoval, existsLocallyNotRemotelyPendingRemoval)));
        verifyRemoteTrackLikeAddition(times(1), existsLocallyNotRemotely);
        verifyRemoteTrackLikeRemoval(times(1), existsRemotelyPendingRemoval);
    }

    @Test
    public void shouldResolveNewlyLikedTrackUrnsToFullTracksAndStoreThemLocally() throws Exception {
        withLocalTrackLikes();
        withLocalPlaylistLikes();
        withRemoteTrackLikes(trackLike);
        withRemotePlaylistLikes();
        ApiTrackCollection trackResponse = new ApiTrackCollection();
        trackResponse.setCollection(Arrays.asList(ModelFixtures.create(ApiTrack.class)));
        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("GET", ApiEndpoints.TRACKS.path())
                        .withQueryParam("urns", trackLike.getTargetUrn().toEncodedString()))))
                .thenReturn(trackResponse);

        syncer.syncContent(null, null);

        verify(trackWriteStorage).storeTracks(trackResponse.getCollection());
    }

    @Test
    public void shouldResolveNewlyLikedPlaylistUrnsToFullPlaylistsAndStoreThemLocally() throws Exception {
        withLocalTrackLikes();
        withLocalPlaylistLikes();
        withRemoteTrackLikes();
        withRemotePlaylistLikes(playlistLike);
        ApiPlaylistCollection trackResponse = new ApiPlaylistCollection();
        trackResponse.setCollection(Arrays.asList(ModelFixtures.create(ApiPlaylist.class)));
        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("GET", ApiEndpoints.PLAYLISTS.path())
                        .withQueryParam("urns", playlistLike.getTargetUrn().toEncodedString()))))
                .thenReturn(trackResponse);

        syncer.syncContent(null, null);

        verify(playlistWriteStorage).storePlaylists(trackResponse.getCollection());
    }

    private void withRemoteTrackLikes(ApiLike... likes) throws Exception {
        final ModelCollection<ApiLike> response = new ModelCollection<>(Arrays.asList(likes));
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.LIKED_TRACKS.path(userUrn))))).thenReturn(response);
    }

    private void withRemotePlaylistLikes(ApiLike... likes) throws Exception {
        final ModelCollection<ApiLike> response = new ModelCollection<>(Arrays.asList(likes));
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.LIKED_PLAYLISTS.path(userUrn))))).thenReturn(response);
    }

    private void withLocalTrackLikes() {
        when(likesStorage.loadTrackLikes()).thenReturn(Collections.<PropertySet>emptyList());
    }

    private void withLocalTrackLikes(ApiLike... likes) {
        when(likesStorage.loadTrackLikes()).thenReturn(CollectionUtils.toPropertySets(Arrays.asList(likes)));
    }

    private void withLocalTrackLikes(PropertySet... likes) {
        when(likesStorage.loadTrackLikes()).thenReturn(Arrays.asList(likes));
    }

    private void withLocalTrackLikesPendingRemoval(PropertySet... likes) {
        when(likesStorage.loadTrackLikesPendingRemoval()).thenReturn(Arrays.asList(likes));
    }

    private void withLocalPlaylistLikes() {
        when(likesStorage.loadPlaylistLikes()).thenReturn(Collections.<PropertySet>emptyList());
    }

    private void withLocalPlaylistLikes(ApiLike... likes) {
        when(likesStorage.loadPlaylistLikes()).thenReturn(CollectionUtils.toPropertySets(Arrays.asList(likes)));
    }

    private void withLocalPlaylistLikesPendingRemoval(PropertySet... likes) {
        when(likesStorage.loadPlaylistLikesPendingRemoval()).thenReturn(Arrays.asList(likes));
    }

    private void verifyRemoteTrackLikeAddition(VerificationMode verificationMode, ApiLike like) {
        verify(apiClient, verificationMode).fetchResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.MY_TRACK_LIKES.path(like.getTargetUrn().getNumericId()))));
    }

    private void verifyRemoteTrackLikeRemoval(VerificationMode verificationMode, ApiLike like) {
        verify(apiClient, verificationMode).fetchResponse(
                argThat(isPublicApiRequestTo("DELETE", ApiEndpoints.MY_TRACK_LIKES.path(like.getTargetUrn().getNumericId()))));
    }

    private void verifyRemotePlaylistLikeAddition(VerificationMode verificationMode, ApiLike like) {
        verify(apiClient, verificationMode).fetchResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.MY_PLAYLIST_LIKES.path(like.getTargetUrn().getNumericId()))));
    }

    private void verifyRemotePlaylistLikeRemoval(VerificationMode verificationMode, ApiLike like) {
        verify(apiClient, verificationMode).fetchResponse(
                argThat(isPublicApiRequestTo("DELETE", ApiEndpoints.MY_PLAYLIST_LIKES.path(like.getTargetUrn().getNumericId()))));
    }
}