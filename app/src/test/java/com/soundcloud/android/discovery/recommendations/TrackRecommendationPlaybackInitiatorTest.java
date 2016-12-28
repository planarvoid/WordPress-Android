package com.soundcloud.android.discovery.recommendations;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.discovery.EmptyViewItem;
import com.soundcloud.android.discovery.PlaylistTagsItem;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
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

    private static final DiscoveryItem EMPTY = EmptyViewItem.fromThrowable(new RuntimeException("expected"));
    private static final RecommendedTracksBucketItem FIRST_RECOMMENDATIONS = RecommendedTracksBucketItem.create(
            createSeed(SEED_1, FIRST_QUERY_POSITION), singletonList(RECOMMENDATION_1));
    private static final RecommendedTracksBucketItem SECOND_RECOMMENDATIONS = RecommendedTracksBucketItem.create(
            createSeed(SEED_2, SECOND_QUERY_POSITION), singletonList(RECOMMENDATION_2));
    private static final RecommendedTracksBucketItem THIRD_RECOMMENDATIONS = RecommendedTracksBucketItem.create(
            createSeed(SEED_3, THIRD_QUERY_POSITION), singletonList(RECOMMENDATION_3));
    private static final PlaylistTagsItem PLAYLIST_TAGS = PlaylistTagsItem.create(
            Collections.<String>emptyList(), Collections.<String>emptyList());
    private static final DiscoveryItem FOOTER = DiscoveryItem.forRecommendedTracksFooter();

    private static final List<DiscoveryItem> DISCOVERY_ITEMS =
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

    private static PropertySet createSeed(Urn urn, int seedQueryPosition) {
        PropertySet seed = PropertySet.create();
        seed.put(RecommendationProperty.SEED_TRACK_URN, urn);
        seed.put(RecommendationProperty.QUERY_POSITION, seedQueryPosition);
        seed.put(RecommendationProperty.QUERY_URN, QUERY_URN);
        return seed;
    }

    private static Recommendation createRecommendation(Urn seedUrn, Urn recommendationUrn, int queryPosition) {
        TrackItem trackItem = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        trackItem.setUrn(recommendationUrn);
        return new Recommendation(trackItem, seedUrn, false, queryPosition, QUERY_URN);
    }

}
