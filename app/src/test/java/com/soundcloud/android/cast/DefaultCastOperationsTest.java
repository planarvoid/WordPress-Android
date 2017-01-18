package com.soundcloud.android.cast;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultCastOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);
    private static final String TOKEN = "fakeToken";

    private DefaultCastOperations castOperations;

    @Mock private TrackRepository trackRepository;
    @Mock private PolicyOperations policyOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private CastJsonHandler castJsonHandler;
    @Mock private CastQueueController castQueueController;
    @Mock private AccountOperations accountOperations;

    private TestSubscriber<List<Urn>> listUrnTestSubscriber;
    private TestSubscriber<LoadMessageParameters> loadMessageParametersTestSubscriber;

    @Before
    public void setUp() throws Exception {
        castOperations = new DefaultCastOperations(trackRepository,
                                                   policyOperations,
                                                   playQueueManager,
                                                   castJsonHandler,
                                                   castQueueController,
                                                   accountOperations);
        listUrnTestSubscriber = new TestSubscriber<>();
        loadMessageParametersTestSubscriber = new TestSubscriber<>();
        when(castJsonHandler.toJson(any(CastPlayQueue.class))).thenReturn(new JSONObject());
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token(TOKEN, null));
    }

    @Test
    public void createsLoadRequestParametersAsExpected() {
        long playPosition = 235L;
        boolean autoplay = true;
        createAndSetupPublicTrack(TRACK1);
        createAndSetupPublicTrack(TRACK2);
        List<Urn> playQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(playQueueTracks)).thenReturn(Observable.just(playQueueTracks));
        when(castQueueController.buildCastPlayQueue(TRACK1, playQueueTracks)).thenReturn(new CastPlayQueue(TRACK1, playQueueTracks));

        castOperations.createLoadMessageParameters(TRACK1, autoplay, playPosition, playQueueTracks).subscribe(loadMessageParametersTestSubscriber);

        assertThat(loadMessageParametersTestSubscriber.getOnNextEvents()).hasSize(1);

        LoadMessageParameters loadMessageParameters = loadMessageParametersTestSubscriber.getOnNextEvents().get(0);
        assertThat(loadMessageParameters.autoplay).isEqualTo(autoplay);
        assertThat(loadMessageParameters.playPosition).isEqualTo(playPosition);
        assertThat(loadMessageParameters.jsonData).isNotNull();
    }

    @Test
    public void loadRequestIsCreatedWithAttachedCredentials() {
        CastPlayQueue castPlayQueue = mock(CastPlayQueue.class);
        List<Urn> filteredTracks = Arrays.asList(TRACK1, TRACK2);
        when(castQueueController.buildCastPlayQueue(any(Urn.class), anyList())).thenReturn(castPlayQueue);
        when(policyOperations.filterMonetizableTracks(anyList())).thenReturn(Observable.just(filteredTracks));

        castOperations.createLoadMessageParameters(createAndSetupPublicTrack(TRACK1).getUrn(), false, 0L, filteredTracks).subscribe(loadMessageParametersTestSubscriber);

        verify(accountOperations).getSoundCloudToken();
        verify(castPlayQueue).setCredentials(isA(CastCredentials.class));
    }

    @Test
    public void filteringTracksRemoveMonetizableTracksFromList() throws JSONException {
        createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        List<Urn> filteredPlayQueueTracks = singletonList(TRACK1);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(filteredPlayQueueTracks));

        castOperations.filterTracksToBePlayedRemotely(TRACK1, unfilteredPlayQueueTracks).subscribe(listUrnTestSubscriber);

        assertThat(listUrnTestSubscriber.getOnNextEvents()).hasSize(1);
        assertThat(listUrnTestSubscriber.getOnNextEvents().get(0)).isEqualTo(filteredPlayQueueTracks);
    }

    @Test
    public void filteringTracksWhenTryingToPlayAMonetizableTrackReturnsEmptyQueue() throws JSONException {
        createAndSetupPublicTrack(TRACK2);
        TrackItem currentTrackBeforeFiltering = createAndSetupPublicTrack(TRACK3);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2, TRACK3);
        List<Urn> filteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(filteredPlayQueueTracks));

        castOperations.filterTracksToBePlayedRemotely(currentTrackBeforeFiltering.getUrn(), unfilteredPlayQueueTracks).subscribe(listUrnTestSubscriber);

        assertThat(listUrnTestSubscriber.getOnNextEvents()).hasSize(1);
        assertThat(listUrnTestSubscriber.getOnNextEvents().get(0)).isEqualTo(emptyList());
    }

    @Test
    public void filteringTracksEmitsEmptyListOfUrnsWhenAllTracksAreFilteredOut() {
        List<Urn> unfilteredPlayQueueTracks = singletonList(TRACK1);
        List<Urn> filteredPlayQueueTracks = Collections.emptyList();
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(filteredPlayQueueTracks));

        castOperations.filterTracksToBePlayedRemotely(TRACK1, unfilteredPlayQueueTracks).subscribe(listUrnTestSubscriber);

        assertThat(listUrnTestSubscriber.getOnNextEvents()).hasSize(1);
        assertThat(listUrnTestSubscriber.getOnNextEvents().get(0).isEmpty()).isTrue();
    }

    @Test
    public void subscriberHandlesTheErrorIfJsonParsingExceptionIsThrownForCastPlayQueueWhileCreatingLoadParameters() {
        createAndSetupPublicTrack(TRACK1);
        when(castJsonHandler.toJson(any(CastPlayQueue.class))).thenThrow(IllegalArgumentException.class);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2, TRACK3);
        List<Urn> filteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(filteredPlayQueueTracks));

        castOperations.createLoadMessageParameters(TRACK1, true, 123L, unfilteredPlayQueueTracks).subscribe(loadMessageParametersTestSubscriber);

        assertThat(loadMessageParametersTestSubscriber.getOnNextEvents()).isEmpty();
        assertThat(loadMessageParametersTestSubscriber.getOnErrorEvents()).hasSize(1);
    }

    private TrackItem createAndSetupPublicTrack(Urn urn) {
        TrackItem track = TestPropertySets.trackWith(PropertySet.from(
                TrackProperty.URN.bind(urn),
                TrackProperty.TITLE.bind("Title " + urn),
                TrackProperty.CREATOR_NAME.bind("Creator " + urn),
                TrackProperty.IS_PRIVATE.bind(false)));
        when(trackRepository.track(urn)).thenReturn(Observable.just(track));
        return track;
    }
}
