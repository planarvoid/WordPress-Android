package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.events.EntityStateChangedEvent.fromLike;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static com.soundcloud.java.collections.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

// AndroidUnitTest because of PropertySets being used
public class LikesSyncerTest extends AndroidUnitTest {

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
    @Captor private ArgumentCaptor<Collection<PropertySet>> removedLikes;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setup() throws Exception {
        syncer = new LikesSyncer(fetchLikes, fetchLikedResources, pushLikeAdditions, pushLikeDeletions, loadLikes,
                                 loadLikesPendingAddition, loadLikesPendingRemoval, storeLikedResources, storeLikes,
                                 removeLikes, eventBus, TYPE_TRACK);
        trackLike = ModelFixtures.apiTrackLike();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
    }

    @Test
    public void shouldDoNothingIfLocalAndRemoteStateAreIdentical() throws Exception {
        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes(trackLike);

        assertThat(syncer.call()).isFalse();

        verifyZeroInteractions(removeLikes);
        verifyZeroInteractions(storeLikes);
        verify(pushLikeAdditions, never()).call();
        verify(pushLikeDeletions, never()).call();
    }

    @Test
    public void shouldAddLikeRemotelyIfItExistsLocallyWithPendingAdditionAndNotRemotely() throws Exception {
        withRemoteTrackLikes();
        withLocalTrackLikesPendingAddition(trackLike.toPropertySet());

        assertThat(syncer.call()).isFalse();

        verifyZeroInteractions(removeLikes);
        verify(pushLikeAdditions).call();
        verify(pushLikeDeletions, never()).call();
    }

    @Test
    public void shouldWriteSuccessfulRemoteAdditionsBackToLocalStorageWithUpdatedTimestamps() throws Exception {
        when(pushLikeAdditions.call()).thenReturn(singleton(trackLike.toPropertySet()));
        withRemoteTrackLikes();
        withLocalTrackLikesPendingAddition(trackLike.toPropertySet());

        assertThat(syncer.call()).isTrue();

        verify(storeLikes).call(singleton(trackLike.toPropertySet()));
    }

    @Test
    public void shouldNotFetchEntityForPushedAdditionAsItWillOverwriteLocalStats() throws Exception {
        when(pushLikeAdditions.call()).thenReturn(singleton(trackLike.toPropertySet()));
        withRemoteTrackLikes();
        withLocalTrackLikesPendingAddition(trackLike.toPropertySet());

        assertThat(syncer.call()).isTrue();

        verify(fetchLikedResources, never()).call();
    }

    @Test
    public void shouldRemoveLikeLocallyIfExistsLocallyButNotRemotely() throws Exception {
        withRemoteTrackLikes();
        withLocalTrackLikes(trackLike);

        assertThat(syncer.call()).isTrue();

        verify(pushLikeAdditions, never()).call();
        verify(pushLikeDeletions, never()).call();

        verify(removeLikes).call(removedLikes.capture());
        assertThat(removedLikes.getValue()).hasSize(1);
        assertThat(removedLikes.getValue().iterator().next().get(LikeProperty.TARGET_URN))
                .isEqualTo(trackLike.getTargetUrn());
        verifyZeroInteractions(storeLikes);
    }

    @Test
    public void shouldCreateLikeLocallyIfExistsRemotelyButNotLocally() throws Exception {
        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();

        assertThat(syncer.call()).isTrue();

        verify(storeLikes).call(singleton(trackLike.toPropertySet()));
        verifyZeroInteractions(removeLikes);
        verify(pushLikeAdditions, never()).call();
        verify(pushLikeDeletions, never()).call();
    }

    @Test
    public void shouldRemoveLikeRemotelyIfLocalLikeIsPendingRemovalAndExistsRemotelyWithOlderTimestamp() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                                                       .put(LikeProperty.REMOVED_AT,
                                                            new Date(trackLike.getCreatedAt().getTime() + 1));
        PropertySet deletedLike = PropertySet.from(LikeProperty.TARGET_URN.bind(trackLike.getTargetUrn()));

        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        when(pushLikeDeletions.call()).thenReturn(singleton(deletedLike));

        assertThat(syncer.call()).isTrue();
        assertThat(pushLikeDeletions.getInput()).containsExactly(trackLikePendingRemoval);
        verify(pushLikeDeletions).call();
        verify(pushLikeAdditions, never()).call();
    }

    @Test
    public void shouldRemoveLikeLocallyIfPendingRemovalRequestSucceeded() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                                                       .put(LikeProperty.REMOVED_AT,
                                                            new Date(trackLike.getCreatedAt().getTime() + 1));
        PropertySet deletedLike = PropertySet.from(LikeProperty.TARGET_URN.bind(trackLike.getTargetUrn()));

        withRemoteTrackLikes(trackLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);
        when(pushLikeDeletions.call()).thenReturn(singleton(deletedLike));

        assertThat(syncer.call()).isTrue();

        final Urn removedUrn = trackLikePendingRemoval.get(LikeProperty.TARGET_URN);
        verify(removeLikes).call(singleton(PropertySet.from(LikeProperty.TARGET_URN.bind(removedUrn))));
        verifyZeroInteractions(storeLikes);
    }

    @Test
    public void shouldNotRemoveLikeLocallyIfPendingRemovalRequestFailed() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                                                       .put(LikeProperty.REMOVED_AT,
                                                            new Date(trackLike.getCreatedAt().getTime() + 1));
        final ApiLike otherLike = ModelFixtures.apiTrackLike();
        PropertySet otherLikePendingRemoval = otherLike.toPropertySet()
                                                       .put(LikeProperty.REMOVED_AT,
                                                            new Date(otherLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes(trackLike, otherLike);
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval, otherLikePendingRemoval);
        when(pushLikeDeletions.call()).thenReturn(singleton(otherLikePendingRemoval));

        assertThat(syncer.call()).isTrue();

        // only remove the second like (first one failed)
        Urn likeToRemove = otherLikePendingRemoval.get(LikeProperty.TARGET_URN);
        verify(removeLikes).call(removedLikes.capture());
        assertThat(removedLikes.getValue()).hasSize(1);
        assertThat(removedLikes.getValue().iterator().next().get(LikeProperty.TARGET_URN)).isEqualTo(likeToRemove);
        verifyZeroInteractions(storeLikes);
    }

    @Test
    public void shouldRemoveLikeOnlyLocallyIfPendingRemovalAndDoesNotExistRemotely() throws Exception {
        PropertySet trackLikePendingRemoval = trackLike.toPropertySet()
                                                       .put(LikeProperty.REMOVED_AT,
                                                            new Date(trackLike.getCreatedAt().getTime() + 1));

        withRemoteTrackLikes();
        withLocalTrackLikes();
        withLocalTrackLikesPendingRemoval(trackLikePendingRemoval);

        assertThat(syncer.call()).isTrue();

        Urn likeToRemove = trackLikePendingRemoval.get(LikeProperty.TARGET_URN);
        verify(removeLikes).call(removedLikes.capture());
        assertThat(removedLikes.getValue()).hasSize(1);
        assertThat(removedLikes.getValue().iterator().next().get(LikeProperty.TARGET_URN)).isEqualTo(likeToRemove);
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
                                                                .put(LikeProperty.ADDED_AT,
                                                                     new Date(existsRemotelyPendingRemoval.getCreatedAt()
                                                                                                          .getTime() + 1));

        PropertySet existsLocallyPendingRemoval = existsRemotelyPendingRemoval.toPropertySet()
                                                                              .put(LikeProperty.REMOVED_AT,
                                                                                   new Date(existsRemotelyPendingRemoval
                                                                                                    .getCreatedAt()
                                                                                                    .getTime() + 1));
        PropertySet existsLocallyNotRemotelyPendingRemoval =
                ModelFixtures.apiTrackLike().toPropertySet()
                             .put(LikeProperty.REMOVED_AT, new Date());
        when(pushLikeDeletions.call()).thenReturn(singleton(existsLocallyPendingRemoval));

        // assertThated outcome:
        // - one local addition
        // - three local removals
        // - one remote addition
        // - one remote removal
        withRemoteTrackLikes(existsRemotelyNotLocally, existsRemotelyPendingRemoval);
        withLocalTrackLikes(existsLocallyNotRemotely.toPropertySet());
        withLocalTrackLikesPendingAddition(existsLocallyPendingAddition);
        withLocalTrackLikesPendingRemoval(existsLocallyPendingRemoval, existsLocallyNotRemotelyPendingRemoval);

        assertThat(syncer.call()).isTrue();

        verify(storeLikes).call(singleton(existsRemotelyNotLocally.toPropertySet()));
        verify(removeLikes).call(newHashSet(existsLocallyPendingRemoval,
                                            existsLocallyNotRemotely.toPropertySet(),
                                            existsLocallyNotRemotelyPendingRemoval));


        verify(pushLikeAdditions, times(1)).call();
        assertThat(pushLikeAdditions.getInput().iterator().next().get(LikeProperty.TARGET_URN))
                .isEqualTo(newRemoteLike.getTargetUrn());
        verify(pushLikeDeletions, times(1)).call();
        assertThat(pushLikeDeletions.getInput().iterator().next().get(LikeProperty.TARGET_URN))
                .isEqualTo(existsRemotelyPendingRemoval.getTargetUrn());

        assertThat(eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED)).containsExactly(
                fromLike(Collections.singletonList(createLikedEntityChangedProperty(existsRemotelyNotLocally.getTargetUrn()))),
                fromLike(Collections.singletonList(createUnlikedEntityChangedProperty(existsLocallyNotRemotely.getTargetUrn())))
        );
    }

    private PropertySet createLikedEntityChangedProperty(Urn urn) {
        return PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_USER_LIKE.bind(true)
        );
    }

    private PropertySet createUnlikedEntityChangedProperty(Urn urn) {
        return PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_USER_LIKE.bind(false)
        );
    }

    @Test
    public void shouldResolveNewlyLikedResourceUrnsToFullResourcesAndStoreThemLocally() throws Exception {
        withLocalTrackLikes();
        withRemoteTrackLikes(trackLike);
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        when(fetchLikedResources.call()).thenReturn(tracks);

        assertThat(syncer.call()).isTrue();

        verify(storeLikedResources).call(tracks);
    }

    @Test
    public void shouldNotHaveWrittenAdditionsIfResolvingAdditionsFails() throws Exception {
        withLocalTrackLikes();
        withRemoteTrackLikes(trackLike);
        when(fetchLikedResources.call()).thenThrow(new IOException());

        try {
            syncer.call();
        } catch (IOException e) {
            // no op
        }

        verify(storeLikes, never()).call(any(Collection.class));
    }

    private void withRemoteTrackLikes(ApiLike... likes) throws Exception {
        final TreeSet<PropertySet> propertySets = new TreeSet<>(FetchLikesCommand.LIKES_COMPARATOR);
        propertySets.addAll(PropertySets.toPropertySets(likes));
        when(fetchLikes.call()).thenReturn(propertySets);
    }

    private void withLocalTrackLikes() throws Exception {
        when(loadLikes.call(TYPE_TRACK)).thenReturn(Collections.<PropertySet>emptyList());
    }

    private void withLocalTrackLikes(ApiLike... likes) throws Exception {
        when(loadLikes.call(TYPE_TRACK)).thenReturn(PropertySets.toPropertySets(asList(likes)));
    }

    private void withLocalTrackLikes(PropertySet... likes) throws Exception {
        when(loadLikes.call(TYPE_TRACK)).thenReturn(asList(likes));
    }

    private void withLocalTrackLikesPendingRemoval(PropertySet... likes) throws Exception {
        when(loadLikesPendingRemoval.call(TYPE_TRACK)).thenReturn(asList(likes));
    }

    private void withLocalTrackLikesPendingAddition(PropertySet... likes) throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(asList(likes));
    }
}
