package com.soundcloud.android.cast;

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
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.List;

public class DefaultCastOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final String TOKEN = "fakeToken";

    private DefaultCastOperations castOperations;

    @Mock private TrackRepository trackRepository;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private CastJsonHandler castJsonHandler;
    @Mock private CastQueueController castQueueController;
    @Mock private AccountOperations accountOperations;

    private TestSubscriber<LoadMessageParameters> loadMessageParametersTestSubscriber;

    @Before
    public void setUp() throws Exception {
        castOperations = new DefaultCastOperations(trackRepository,
                                                   playQueueManager,
                                                   castJsonHandler,
                                                   castQueueController,
                                                   accountOperations);
        loadMessageParametersTestSubscriber = new TestSubscriber<>();

        when(castJsonHandler.toJson(any(CastPlayQueue.class))).thenReturn(new JSONObject());
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token(TOKEN, null));
    }

    @Test
    public void createsLoadRequestParametersAsExpected() {
        final long playPosition = 235L;
        final boolean autoplay = true;
        final List<Urn> playQueueTracks = Arrays.asList(TRACK1, TRACK2);
        createAndSetupPublicTrack(TRACK1);
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

        castOperations.createLoadMessageParameters(createAndSetupPublicTrack(TRACK1).getUrn(), false, 0L, filteredTracks).subscribe(loadMessageParametersTestSubscriber);

        verify(accountOperations).getSoundCloudToken();
        verify(castPlayQueue).setCredentials(isA(CastCredentials.class));
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
