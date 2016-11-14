package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlayQueue.fromPlayableList;
import static com.soundcloud.android.playback.PlayQueue.fromRecommendations;
import static com.soundcloud.android.playback.PlayQueue.fromTrackUrnList;
import static com.soundcloud.android.playback.PlaySessionSource.forRecommendations;
import static com.soundcloud.android.playback.PlaySessionSource.forStation;
import static com.soundcloud.android.playback.PlaybackContext.create;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class PlayQueueTest extends AndroidUnitTest {

    @Test
    public void fromStationShouldApplyPlaybackContextShouldApplyPlaybackContext() {
        final Urn queryUrn = new Urn("soundcloud:query:2");
        final StationTrack stationTrack = StationTrack.create(Urn.forTrack(1), queryUrn);
        final Urn station = Urn.forTrackStation(3L);
        final PlaySessionSource playSessionSource = forStation(Screen.STATIONS_INFO, station);

        final PlayQueue playQueue = PlayQueue.fromStation(station,
                                                          singletonList(stationTrack),
                                                          playSessionSource);

        assertThat(playbackContext(playQueue)).isEqualTo(create(playSessionSource));
    }

    @Test
    public void fromRecommendationsShouldApplyPlaybackContext() {
        final Urn queryUrn = new Urn("soundcloud:query:2");
        final PlaySessionSource playSessionSource = forRecommendations(Screen.SEARCH_SUGGESTIONS, 0, queryUrn);
        final ApiTrack recommendedTrack = ModelFixtures.create(ApiTrack.class);
        final RecommendedTracksCollection relatedTracks = new RecommendedTracksCollection(singletonList(recommendedTrack),
                                                                                          "0");
        final PlayQueue playQueue = fromRecommendations(Urn.forTrack(1L), false, relatedTracks, playSessionSource);

        assertThat(playbackContext(playQueue)).isEqualTo(create(playSessionSource));
    }

    @Test
    public void fromRecommendationsForContinuousPlayShouldApplyAutoPlayPlaybackContext() {
        final Urn queryUrn = new Urn("soundcloud:query:2");
        final PlaySessionSource playSessionSource = forRecommendations(Screen.SEARCH_SUGGESTIONS, 0, queryUrn);
        final ApiTrack recommendedTrack = ModelFixtures.create(ApiTrack.class);
        final RecommendedTracksCollection relatedTracks = new RecommendedTracksCollection(singletonList(recommendedTrack),
                                                                                          "0");
        final PlayQueue playQueue = fromRecommendations(Urn.forTrack(1L), true, relatedTracks, playSessionSource);

        assertThat(playbackContext(playQueue)).isEqualTo(create(PlaybackContext.Bucket.AUTO_PLAY));
    }

    @Test
    public void fromPlayableListShouldApplyPlaybackContext() {
        final PropertySet track = TestPropertySets.fromApiTrack();
        final PropertySet playlist = TestPropertySets.fromApiPlaylist();
        final List<PropertySet> playables = asList(track, playlist);
        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.STREAM);

        final PlayQueue playQueue = fromPlayableList(playables,
                                                     playSessionSource,
                                                     Collections.<Urn, Boolean>emptyMap());

        final PlaybackContext expected = create(playSessionSource);

        assertThat(playbackContext(playQueue, 0)).isEqualTo(expected);
        assertThat(playbackContext(playQueue, 1)).isEqualTo(expected);
    }

    @Test
    public void fromTrackUrnListShouldApplyPlaybackContext() {
        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.STREAM);

        final PlayQueue playQueue = fromTrackUrnList(singletonList(Urn.forTrack(123L)),
                                                     playSessionSource,
                                                     Collections.<Urn, Boolean>emptyMap());

        final PlaybackContext context = create(playSessionSource);

        assertThat(playbackContext(playQueue)).isEqualTo(context);
    }

    private PlaybackContext playbackContext(PlayQueue playQueue) {
        return playbackContext(playQueue, 0);
    }

    private PlaybackContext playbackContext(PlayQueue playQueue, int position) {
        return (playableQueueItem(playQueue, position)).getPlaybackContext();
    }

    private PlayableQueueItem playableQueueItem(PlayQueue playQueue, int position) {
        return (PlayableQueueItem) (playQueue.getPlayQueueItem(position));
    }
}