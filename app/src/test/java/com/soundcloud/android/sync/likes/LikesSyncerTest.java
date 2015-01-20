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

@RunWith(SoundCloudTestRunner.class)
public class LikesSyncerTest {

    private LikesSyncer syncer;

    private final Urn userUrn = new Urn("soundcloud:users:123");
    private ApiLike trackLike;

    @Mock private ApiClient apiClient;
    @Mock private LoadLikesCommand loadLikes;
    @Mock private LoadLikesPendingAdditionCommand loadLikesPendingAddition;
    @Mock private LoadLikesPendingRemovalCommand loadLikesPendingRemoval;
    @Mock private ApiResourceCommand fetchLikedResources;
    @Mock private StoreCommand storeLikedResources;
    @Mock private StoreLikesCommand storeLikes;
    @Mock private RemoveLikesCommand removeLikes;
    @Mock private AccountOperations accountOperations;

    @Before
    public void setup() throws Exception {
        syncer = new LikesSyncer(apiClient, fetchLikedResources, loadLikes,
                loadLikesPendingAddition, loadLikesPendingRemoval, storeLikedResources, storeLikes,
                removeLikes, accountOperations, ApiEndpoints.LIKED_TRACKS, ApiEndpoints.MY_TRACK_LIKES);
        trackLike = ModelFixtures.apiTrackLike();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
    }

    @Test
    public void shouldDoNothingIfLocalAndRemoteStateAreIdentical() throws Exception {
        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes(trackLike);

        expect(syncer.call()).toBe(false);

        verifyZeroInteractions(removeLikes);
        verifyZeroInteractions(storeLikes);
        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
    }

    @Test
    public void shouldAddLikeRemotelyIfItExistsLocallyWithPendingAdditionAndNotRemotely() throws Exception {
        withRemoteTrackLikes();
        withLocalTrackLikesPendingAddition(trackLike.toPropertySet());

        expect(syncer.call()).toBe(false);

        verifyZeroInteractions(removeLikes);
        verifyZeroInteractions(storeLikes);
        verifyRemoteTrackLikeAddition(times(1), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);
    }

    @Test
    public void shouldRemoveLikeLocallyIfExistsLocallyButNotRemotely() throws Exception {
        withRemoteTrackLikes();
        withLocalTrackLikes(trackLike);

        expect(syncer.call()).toBe(true);

        verifyRemoteTrackLikeAddition(never(), trackLike);
        verifyRemoteTrackLikeRemoval(never(), trackLike);

        expect(removeLikes.getInput()).toNumber(1);
        Urn removedUrn = removeLikes.getInput().iterator().next().get(LikeProperty.TARGET_URN);
        expect(removedUrn).toEqual(trackLike.getTargetUrn());
        verify(removeLikes).call();
        verifyZeroInteractions(storeLikes);
    }

    @Test
    public void shouldCreateLikeLocallyIfExistsRemotelyButNotLocally() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class))).thenReturn(new ApiTrackCollection());
        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();

        expect(syncer.call()).toBe(true);

        expect(storeLikes.getInput()).toContainExactly(trackLike.toPropertySet());
        verify(storeLikes).call();
        verifyZeroInteractions(removeLikes);
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

        expect(syncer.call()).toBe(true);

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

        expect(syncer.call()).toBe(true);

        expect(removeLikes.getInput()).toNumber(1);
        Urn removedUrn = removeLikes.getInput().iterator().next().get(LikeProperty.TARGET_URN);
        expect(removedUrn).toEqual(trackLikePendingRemoval.get(LikeProperty.TARGET_URN));
        verify(removeLikes).call();
        verifyZeroInteractions(storeLikes);
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

        expect(syncer.call()).toBe(true);

        // only remove the second like (first one failed)
        expect(removeLikes.getInput()).toNumber(1);
        Urn removedUrn = removeLikes.getInput().iterator().next().get(LikeProperty.TARGET_URN);
        expect(removedUrn).toEqual(otherLikePendingRemoval.get(LikeProperty.TARGET_URN));
        verify(removeLikes).call();
        verifyZeroInteractions(storeLikes);
    }

    @Test
    public void shouldRemoveLikeOnlyLocallyIfPendingRemovalAndDoesNotExistRemotely() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes();
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);

        expect(syncer.call()).toBe(true);

        expect(removeLikes.getInput()).toNumber(1);
        Urn removedUrn = removeLikes.getInput().iterator().next().get(LikeProperty.TARGET_URN);
        expect(removedUrn).toEqual(trackLikePendingRemoval.get(LikeProperty.TARGET_URN));

        verify(removeLikes).call();
        verifyZeroInteractions(storeLikes);
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

        expect(syncer.call()).toBe(true);

        expect(storeLikes.getInput()).toContainExactly(trackLike.toPropertySet());
        verify(storeLikes).call();
        verifyZeroInteractions(removeLikes);
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

        ApiLike newRemoteLike = ModelFixtures.apiTrackLike();
        PropertySet existsLocallyPendingAddition = newRemoteLike.toPropertySet()
                .put(LikeProperty.ADDED_AT, new Date(existsRemotelyPendingRemoval.getCreatedAt().getTime() + 1));

        PropertySet existsLocallyPendingRemoval = existsRemotelyPendingRemoval.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(existsRemotelyPendingRemoval.getCreatedAt().getTime() + 1));
        PropertySet existsLocallyNotRemotelyPendingRemoval =
                ModelFixtures.apiTrackLike().toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date());
        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(TestApiResponses.ok());

        // expected outcome:
        // - one local addition
        // - three local removals
        // - one remote addition
        // - one remote removal
        withRemoteTrackLikes(existsRemotelyNotLocally, existsRemotelyPendingRemoval);
        withLocalTrackLikes(existsLocallyNotRemotely.toPropertySet());
        withLocalTrackLikesPendingAddition(existsLocallyPendingAddition);
        withLocalTrackLikesPendingRemoval(existsLocallyPendingRemoval, existsLocallyNotRemotelyPendingRemoval);

        expect(syncer.call()).toBe(true);

        verify(storeLikes).call();
        expect(storeLikes.getInput()).toContainExactly(existsRemotelyNotLocally.toPropertySet());
        verify(removeLikes).call();
        expect(removeLikes.getInput()).toContainExactly(existsLocallyPendingRemoval, existsLocallyNotRemotely.toPropertySet(), existsLocallyNotRemotelyPendingRemoval);

        verifyRemoteTrackLikeAddition(times(1), newRemoteLike);
        verifyRemoteTrackLikeRemoval(times(1), existsRemotelyPendingRemoval);
    }

    @Test
    public void shouldResolveNewlyLikedResourceUrnsToFullResourcesAndStoreThemLocally() throws Exception {
        withLocalTrackLikes();
        withRemoteTrackLikes(trackLike);
        final ApiTrackCollection tracks = new ApiTrackCollection();
        tracks.setCollection(ModelFixtures.create(ApiTrack.class, 2));
        when(fetchLikedResources.call()).thenReturn(tracks);

        expect(syncer.call()).toBe(true);

        verify(storeLikedResources).call();
        expect(storeLikedResources.getInput()).toEqual(tracks);
    }

    private void withRemoteTrackLikes(ApiLike... likes) throws Exception {
        final ModelCollection<ApiLike> response = new ModelCollection<>(Arrays.asList(likes));
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.LIKED_TRACKS.path(userUrn))))).thenReturn(response);
    }

    private void withLocalTrackLikes() throws Exception {
        when(loadLikes.call()).thenReturn(Collections.<PropertySet>emptyList());
    }

    private void withLocalTrackLikes(ApiLike... likes) throws Exception {
        when(loadLikes.call()).thenReturn(toPropertySets(Arrays.asList(likes)));
    }

    private void withLocalTrackLikes(PropertySet... likes) throws Exception {
        when(loadLikes.call()).thenReturn(Arrays.asList(likes));
    }

    private void withLocalTrackLikesPendingRemoval(PropertySet... likes) throws Exception {
        when(loadLikesPendingRemoval.call()).thenReturn(Arrays.asList(likes));
    }

    private void withLocalTrackLikesPendingAddition(PropertySet... likes) throws Exception {
        when(loadLikesPendingAddition.call()).thenReturn(Arrays.asList(likes));
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