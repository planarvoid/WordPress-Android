package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestMethod;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static com.soundcloud.android.utils.CollectionUtils.toPropertySets;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.ApiResourceCommand;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.likes.ApiLike;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestApiResponses;
import com.soundcloud.android.tracks.ApiTrackCollection;
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

    @Mock private ApiClient apiClient;
    @Mock private LoadLikesCommand loadLikesCommand;
    @Mock private LoadLikesPendingRemovalCommand loadLikesPendingRemovalCommand;
    @Mock private ApiResourceCommand fetchLikedResourcesCommand;
    @Mock private StoreCommand storeLikedResourcesCommand;
    @Mock private StoreLikesCommand storeLikesCommand;
    @Mock private RemoveLikesCommand removeLikesCommand;
    @Mock private AccountOperations accountOperations;

    @Before
    public void setup() throws Exception {
        syncer = new LikesSyncer(apiClient, fetchLikedResourcesCommand, loadLikesCommand,
                loadLikesPendingRemovalCommand, storeLikedResourcesCommand, storeLikesCommand,
                removeLikesCommand, accountOperations, ApiEndpoints.LIKED_TRACKS, ApiEndpoints.MY_TRACK_LIKES);
        trackLike = ModelFixtures.apiTrackLike();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
    }

    @Test
    public void shouldDoNothingIfLocalAndRemoteStateAreIdentical() throws Exception {
        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes(trackLike);

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.UNCHANGED);

        verifyZeroInteractions(removeLikesCommand);
        verifyZeroInteractions(storeLikesCommand);
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
    }

    @Test
    public void shouldCreateLikeRemotelyIfExistsLocallyButNotRemotely() throws Exception {
        withRemoteTrackLikes();
        withLocalTrackLikes(trackLike);

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.UNCHANGED);

        verifyRemoteTrackLikeAddition(times(1), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
        verifyZeroInteractions(removeLikesCommand);
        verifyZeroInteractions(storeLikesCommand);
    }

    @Test
    public void shouldCreateLikeLocallyIfExistsRemotelyButNotLocally() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class))).thenReturn(new ApiTrackCollection());
        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        expect(storeLikesCommand.getInput()).toContainExactly(trackLike.toPropertySet());
        verify(storeLikesCommand).call();
        verifyZeroInteractions(removeLikesCommand);
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
    }

    @Test
    public void shouldRemoveLikeRemotelyIfLocalLikeIsPendingRemovalAndExistsRemotelyWithOlderTimestamp() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(TestApiResponses.ok());

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        verifyRemoteTrackLikeRemoval(times(1), trackLike);
        verifyRemoteTrackLikeAddition(never(), trackLike);
    }

    @Test
    public void shouldRemoveLikeLocallyIfPendingRemovalRequestSucceeded() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(TestApiResponses.ok());

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        expect(removeLikesCommand.getInput()).toNumber(1);
        Urn removedUrn = removeLikesCommand.getInput().iterator().next().get(LikeProperty.TARGET_URN);
        expect(removedUrn).toEqual(trackLikePendingRemoval.get(LikeProperty.TARGET_URN));
        verify(removeLikesCommand).call();
        verifyZeroInteractions(storeLikesCommand);
    }

    @Test
    public void shouldNotRemoveLikeLocallyIfPendingRemovalRequestFailed() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));
        final ApiLike otherLike = ModelFixtures.apiTrackLike();
        PropertySet otherLikePendingRemoval = otherLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(otherLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes(trackLike, otherLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval, otherLikePendingRemoval);
        when(apiClient.fetchResponse(argThat(isPublicApiRequestMethod("DELETE")))).thenReturn(TestApiResponses.status(500), TestApiResponses.ok());

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        // only remove the second like (first one failed)
        expect(removeLikesCommand.getInput()).toNumber(1);
        Urn removedUrn = removeLikesCommand.getInput().iterator().next().get(LikeProperty.TARGET_URN);
        expect(removedUrn).toEqual(otherLikePendingRemoval.get(LikeProperty.TARGET_URN));
        verify(removeLikesCommand).call();
        verifyZeroInteractions(storeLikesCommand);
    }

    @Test
    public void shouldRemoveLikeOnlyLocallyIfPendingRemovalAndDoesNotExistRemotely() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes();
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        expect(removeLikesCommand.getInput()).toNumber(1);
        Urn removedUrn = removeLikesCommand.getInput().iterator().next().get(LikeProperty.TARGET_URN);
        expect(removedUrn).toEqual(trackLikePendingRemoval.get(LikeProperty.TARGET_URN));

        verify(removeLikesCommand).call();
        verifyZeroInteractions(storeLikesCommand);
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
    }

    @Test
    public void shouldReplaceLocalLikePendingRemovalIfRemoteLikeExistsAndHasNewerTimestamp() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class))).thenReturn(new ApiTrackCollection());
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() - 1));

        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        expect(storeLikesCommand.getInput()).toContainExactly(trackLike.toPropertySet());
        verify(storeLikesCommand).call();
        verifyZeroInteractions(removeLikesCommand);
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
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

        ApiSyncResult result = syncer.syncContent(null, null);

        expect(result.success).toBeTrue();
        expect(result.change).toBe(ApiSyncResult.CHANGED);

        verify(storeLikesCommand).call();
        expect(storeLikesCommand.getInput()).toContainExactly(existsRemotelyNotLocally.toPropertySet());
        verify(removeLikesCommand).call();
        expect(removeLikesCommand.getInput()).toContainExactly(existsLocallyPendingRemoval, existsLocallyNotRemotelyPendingRemoval);
        verifyRemoteTrackLikeAddition(times(1), existsLocallyNotRemotely);
        verifyRemoteTrackLikeRemoval(times(1), existsRemotelyPendingRemoval);
    }

    @Test
    public void shouldResolveNewlyLikedResourceUrnsToFullResourcesAndStoreThemLocally() throws Exception {
        withLocalTrackLikes();
        withRemoteTrackLikes(trackLike);
        final ApiTrackCollection tracks = new ApiTrackCollection();
        tracks.setCollection(ModelFixtures.create(ApiTrack.class, 2));
        when(fetchLikedResourcesCommand.call()).thenReturn(tracks);

        syncer.syncContent(null, null);

        verify(storeLikedResourcesCommand).call();
        expect(storeLikedResourcesCommand.getInput()).toEqual(tracks);
    }

    private void withRemoteTrackLikes(ApiLike... likes) throws Exception {
        final ModelCollection<ApiLike> response = new ModelCollection<>(Arrays.asList(likes));
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.LIKED_TRACKS.path(userUrn))))).thenReturn(response);
    }

    private void withLocalTrackLikes() throws Exception {
        when(loadLikesCommand.call()).thenReturn(Collections.<PropertySet>emptyList());
    }

    private void withLocalTrackLikes(ApiLike... likes) throws Exception {
        when(loadLikesCommand.call()).thenReturn(toPropertySets(Arrays.asList(likes)));
    }

    private void withLocalTrackLikes(PropertySet... likes) throws Exception {
        when(loadLikesCommand.call()).thenReturn(Arrays.asList(likes));
    }

    private void withLocalTrackLikesPendingRemoval(PropertySet... likes) throws Exception {
        when(loadLikesPendingRemovalCommand.call()).thenReturn(Arrays.asList(likes));
    }

    private void verifyRemoteTrackLikeAddition(VerificationMode verificationMode, ApiLike like) {
        verify(apiClient, verificationMode).fetchResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.MY_TRACK_LIKES.path(like.getTargetUrn().getNumericId()))));
    }

    private void verifyRemoteTrackLikeRemoval(VerificationMode verificationMode, ApiLike like) {
        verify(apiClient, verificationMode).fetchResponse(
                argThat(isPublicApiRequestTo("DELETE", ApiEndpoints.MY_TRACK_LIKES.path(like.getTargetUrn().getNumericId()))));
    }
}