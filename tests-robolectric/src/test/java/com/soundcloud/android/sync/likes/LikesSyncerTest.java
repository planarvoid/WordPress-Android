package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.PropertySets.toPropertySets;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

@RunWith(SoundCloudTestRunner.class)
public class LikesSyncerTest {

    private LikesSyncer syncer;

    private final Urn userUrn = new Urn("soundcloud:users:123");
    private ApiLike trackLike;

    @Mock private FetchLikesCommand fetchLikes;
    @Mock private LoadLikesCommand loadLikes;
    @Mock private LoadLikesPendingAdditionCommand loadLikesPendingAddition;
    @Mock private LoadLikesPendingRemovalCommand loadLikesPendingRemoval;
    @Mock private BulkFetchCommand fetchLikedResources;
    @Mock private PushLikesCommand<ApiLike> pushLikeAdditions;
    @Mock private PushLikesCommand<ApiDeletedLike> pushLikeDeletions;
    @Mock private StoreTracksCommand storeLikedResources;
    @Mock private StoreLikesCommand storeLikes;
    @Mock private RemoveLikesCommand removeLikes;
    @Mock private AccountOperations accountOperations;

    @Before
    public void setup() throws Exception {
        syncer = new LikesSyncer(fetchLikes, fetchLikedResources, pushLikeAdditions, pushLikeDeletions, loadLikes,
                loadLikesPendingAddition, loadLikesPendingRemoval, storeLikedResources, storeLikes,
                removeLikes);
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
        verify(pushLikeAdditions, never()).call();
        verify(pushLikeDeletions, never()).call();
    }

    @Test
    public void shouldAddLikeRemotelyIfItExistsLocallyWithPendingAdditionAndNotRemotely() throws Exception {
        withRemoteTrackLikes();
        withLocalTrackLikesPendingAddition(trackLike.toPropertySet());

        expect(syncer.call()).toBe(false);

        verifyZeroInteractions(removeLikes);
        verify(pushLikeAdditions).call();
        verify(pushLikeDeletions, never()).call();
    }

    @Test
    public void shouldWriteSuccessfulRemoteAdditionsBackToLocalStorageWithUpdatedTimestamps() throws Exception {
        when(pushLikeAdditions.call()).thenReturn(Collections.singleton(trackLike.toPropertySet()));
        withRemoteTrackLikes();
        withLocalTrackLikesPendingAddition(trackLike.toPropertySet());

        expect(syncer.call()).toBe(true);

        verify(storeLikes).call();
        expect(storeLikes.getInput()).toContainExactly(trackLike.toPropertySet());
    }

    @Test
    public void shouldNotFetchEntityForPushedAdditionAsItWillOverwriteLocalStats() throws Exception {
        when(pushLikeAdditions.call()).thenReturn(Collections.singleton(trackLike.toPropertySet()));
        withRemoteTrackLikes();
        withLocalTrackLikesPendingAddition(trackLike.toPropertySet());

        expect(syncer.call()).toBe(true);

        verify(fetchLikedResources, never()).call();
    }

    @Test
    public void shouldRemoveLikeLocallyIfExistsLocallyButNotRemotely() throws Exception {
        withRemoteTrackLikes();
        withLocalTrackLikes(trackLike);

        expect(syncer.call()).toBe(true);

        verify(pushLikeAdditions, never()).call();
        verify(pushLikeDeletions, never()).call();

        expect(removeLikes.getInput()).toNumber(1);
        Urn removedUrn = removeLikes.getInput().iterator().next().get(LikeProperty.TARGET_URN);
        expect(removedUrn).toEqual(trackLike.getTargetUrn());
        verify(removeLikes).call();
        verifyZeroInteractions(storeLikes);
    }

    @Test
    public void shouldCreateLikeLocallyIfExistsRemotelyButNotLocally() throws Exception {
        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();

        expect(syncer.call()).toBe(true);

        expect(storeLikes.getInput()).toContainExactly(trackLike.toPropertySet());
        verify(storeLikes).call();
        verifyZeroInteractions(removeLikes);
        verify(pushLikeAdditions, never()).call();
        verify(pushLikeDeletions, never()).call();
    }

    @Test
    public void shouldRemoveLikeRemotelyIfLocalLikeIsPendingRemovalAndExistsRemotelyWithOlderTimestamp() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));
        PropertySet deletedLike = PropertySet.from(LikeProperty.TARGET_URN.bind(trackLike.getTargetUrn()));

        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        when(pushLikeDeletions.call()).thenReturn(Collections.singleton(deletedLike));

        expect(syncer.call()).toBe(true);
        expect(pushLikeDeletions.getInput()).toContainExactly(trackLikePendingRemoval);
        verify(pushLikeDeletions).call();
        verify(pushLikeAdditions, never()).call();
    }

    @Test
    public void shouldRemoveLikeLocallyIfPendingRemovalRequestSucceeded() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                .put(LikeProperty.REMOVED_AT, new Date(trackLike.getCreatedAt().getTime() + 1));
        PropertySet deletedLike = PropertySet.from(LikeProperty.TARGET_URN.bind(trackLike.getTargetUrn()));

        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        when(pushLikeDeletions.call()).thenReturn(Collections.singleton(deletedLike));

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
        when(pushLikeDeletions.call()).thenReturn(Collections.singleton(otherLikePendingRemoval));

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
        verify(pushLikeAdditions, never()).call();
        verify(pushLikeDeletions, never()).call();
    }

    @Test
    public void mixedScenario() throws Exception {
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
        when(pushLikeDeletions.call()).thenReturn(Collections.singleton(existsLocallyPendingRemoval));

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
        expect(removeLikes.getInput()).toContainExactlyInAnyOrder(existsLocallyPendingRemoval, existsLocallyNotRemotely.toPropertySet(), existsLocallyNotRemotelyPendingRemoval);


        verify(pushLikeAdditions, times(1)).call();
        expect(pushLikeAdditions.getInput().iterator().next().get(LikeProperty.TARGET_URN)).toEqual(newRemoteLike.getTargetUrn());
        verify(pushLikeDeletions, times(1)).call();
        expect(pushLikeDeletions.getInput().iterator().next().get(LikeProperty.TARGET_URN)).toEqual(existsRemotelyPendingRemoval.getTargetUrn());
    }

    @Test
    public void shouldResolveNewlyLikedResourceUrnsToFullResourcesAndStoreThemLocally() throws Exception {
        withLocalTrackLikes();
        withRemoteTrackLikes(trackLike);
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        when(fetchLikedResources.call()).thenReturn(tracks);

        expect(syncer.call()).toBe(true);

        verify(storeLikedResources).call(tracks);
    }

    @Test
    public void shouldNotHaveWrittenAdditionsIfResolvingAdditionsFails() throws Exception {
        withLocalTrackLikes();
        withRemoteTrackLikes(trackLike);
        final ModelCollection<ApiTrack> tracks = new ModelCollection<>();
        tracks.setCollection(ModelFixtures.create(ApiTrack.class, 2));
        when(fetchLikedResources.call()).thenThrow(new IOException());

        try {
            syncer.call();
        } catch (IOException e){
            // no op
        }

        verify(storeLikes, never()).call();
    }

    private void withRemoteTrackLikes(ApiLike... likes) throws Exception {
        final TreeSet<PropertySet> propertySets = new TreeSet<>(FetchLikesCommand.LIKES_COMPARATOR);
        propertySets.addAll(PropertySets.toPropertySets(likes));
        when(fetchLikes.call()).thenReturn(propertySets);
    }

    private void withLocalTrackLikes() throws Exception {
        when(loadLikes.call()).thenReturn(Collections.<PropertySet>emptyList());
    }

    private void withLocalTrackLikes(ApiLike... likes) throws Exception {
        when(loadLikes.call()).thenReturn(PropertySets.toPropertySets(Arrays.asList(likes)));
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
}
