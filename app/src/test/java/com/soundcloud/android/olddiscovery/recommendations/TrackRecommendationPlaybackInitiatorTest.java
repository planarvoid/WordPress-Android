package com.soundcloud.android.olddiscovery.recommendations;

import static com.soundcloud.android.Consts.NOT_SET;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.olddiscovery.OldDiscoveryItem;
import com.soundcloud.android.olddiscovery.EmptyViewItem;
import com.soundcloud.android.olddiscovery.PlaylistTagsItem;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import javax.inject.Provider;
import java.util.Collections;
import java.util.List;

public class TrackRecommendationPlaybackInitiatorTest extends AndroidUnitTest {
    private static final int FIRST_QUERY_POSITION = 1;
    private static final int SECOND_QUERY_POSITION = 2;
    private static final int THIRD_QUERY_POSITION = 3;
    private static final Urn QUERY_URN = new Urn("soundcloud:query:123");
    private static final Urn SEED_1 = new Urn("soundcloud:tracks:seed1");
    private static final Urn SEED_2 = new Urn("soundcloud:tracks:seed2");
    private static final Urn SEED_3 = new Urn("soundcloud:tracks:seed3");
    private static final Urn RECOMMENDATION_URN_1 = new Urn("soundcloud:tracks:recommendation1");
    private static final Urn RECOMMENDATION_URN_2 = new Urn("soundcloud:tracks:recommendation2");
    private static final Urn RECOMMENDATION_URN_3 = new Urn("soundcloud:tracks:recommendation3");
    private static final Recommendation RECOMMENDATION_1 = createRecommendation(
            SEED_1, RECOMMENDATION_URN_1, FIRST_QUERY_POSITION);
    private static final Recommendation RECOMMENDATION_2 = createRecommendation(
            SEED_2, RECOMMENDATION_URN_2, SECOND_QUERY_POSITION);
    private static final Recommendation RECOMMENDATION_3 = createRecommendation(
            SEED_3, RECOMMENDATION_URN_3, THIRD_QUERY_POSITION);

    private static final OldDiscoveryItem EMPTY = EmptyViewItem.fromThrowable(new RuntimeException("expected"));
    private static final RecommendedTracksBucketItem FIRST_RECOMMENDATIONS = RecommendedTracksBucketItem.create(
            createSeed(SEED_1, FIRST_QUERY_POSITION), singletonList(RECOMMENDATION_1));
    private static final RecommendedTracksBucketItem SECOND_RECOMMENDATIONS = RecommendedTracksBucketItem.create(
            createSeed(SEED_2, SECOND_QUERY_POSITION), singletonList(RECOMMENDATION_2));
    private static final RecommendedTracksBucketItem THIRD_RECOMMENDATIONS = RecommendedTracksBucketItem.create(
            createSeed(SEED_3, THIRD_QUERY_POSITION), singletonList(RECOMMENDATION_3));
    private static final PlaylistTagsItem PLAYLIST_TAGS = PlaylistTagsItem.create(
            Collections.emptyList(), Collections.emptyList());
    private static final OldDiscoveryItem FOOTER = OldDiscoveryItem.forRecommendedTracksFooter();

    private static final List<OldDiscoveryItem> DISCOVERY_ITEMS =
            newArrayList(EMPTY, FIRST_RECOMMENDATIONS, SECOND_RECOMMENDATIONS, THIRD_RECOMMENDATIONS, PLAYLIST_TAGS,
                         FOOTER);

    @Mock private PlaybackInitiator playbackInitiator;
    private Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = TestSubscribers.expandPlayerSubscriber();
    private TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator;

    @Before
    public void setUp() {
        trackRecommendationPlaybackInitiator = new TrackRecommendationPlaybackInitiator(expandPlayerSubscriberProvider,
                                                                                        playbackInitiator);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldInitiatePlaybackWithCorrectQueueWhenReasonClicked() {
        when(playbackInitiator.playTracks(any(List.class), anyInt(), any(PlaySessionSource.class))).thenReturn(
                Observable.empty());

        trackRecommendationPlaybackInitiator.playFromReason(SEED_2, Screen.RECOMMENDATIONS_MAIN, DISCOVERY_ITEMS);

        List<Urn> expectedPlaylist = newArrayList(
                RECOMMENDATION_URN_1, SEED_2, RECOMMENDATION_URN_2, RECOMMENDATION_URN_3);
        verify(playbackInitiator).playTracks(
                expectedPlaylist,
                expectedPlaylist.indexOf(SEED_2),
                PlaySessionSource.forRecommendations(Screen.RECOMMENDATIONS_MAIN, SECOND_QUERY_POSITION, QUERY_URN));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldInitiatePlaybackWithCorrectQueueWhenTrackClicked() {
        when(playbackInitiator.playTracks(any(List.class), anyInt(), any(PlaySessionSource.class))).thenReturn(
                Observable.empty());

        trackRecommendationPlaybackInitiator.playFromRecommendation(SEED_2,
                                                                    RECOMMENDATION_URN_2,
                                                                    Screen.RECOMMENDATIONS_MAIN,
                                                                    DISCOVERY_ITEMS);

        List<Urn> expectedPlaylist = newArrayList(
                RECOMMENDATION_URN_1, RECOMMENDATION_URN_2, RECOMMENDATION_URN_3);

        verify(playbackInitiator).playTracks(
                expectedPlaylist,
                expectedPlaylist.indexOf(RECOMMENDATION_URN_2),
                PlaySessionSource.forRecommendations(Screen.RECOMMENDATIONS_MAIN, SECOND_QUERY_POSITION, QUERY_URN));
    }

    private static RecommendationSeed createSeed(Urn urn, int seedQueryPosition) {
        return RecommendationSeed.create(NOT_SET, urn, Strings.EMPTY, RecommendationReason.LIKED, seedQueryPosition, QUERY_URN);
    }

    private static Recommendation createRecommendation(Urn seedUrn, Urn recommendationUrn, int queryPosition) {
        TrackItem trackItem = ModelFixtures.trackItem(recommendationUrn);
        return Recommendation.create(trackItem, seedUrn, false, queryPosition, QUERY_URN);
    }

}