package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.optional.Optional;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimplePlayQueueTest extends AndroidUnitTest {

    private static final TrackQueueItem TRACK_QUEUE_ITEM_1 = TestPlayQueueItem.createTrack(Urn.forTrack(1L),
                                                                                           "source1",
                                                                                           "version1");
    private static final TrackQueueItem TRACK_QUEUE_ITEM_2 = TestPlayQueueItem.createTrack(Urn.forTrack(2L),
                                                                                           "source2",
                                                                                           "version2");
    private static final VideoAdQueueItem VIDEO_QUEUE_ITEM = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(
            722L)));

    private static final int PLAY_QUEUE_ITEM_COUNT = 3;

    private PlayQueue playQueue = PlayQueue.empty();
    private PlaySessionSource playSessionSource;

    @Before
    public void setUp() throws Exception {
        playSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS,
                                                          Urn.forPlaylist(123),
                                                          Urn.forUser(456),
                                                          PLAY_QUEUE_ITEM_COUNT);
        playQueue = PlayQueue.fromPlayQueueItems(newArrayList(TRACK_QUEUE_ITEM_1,
                                                              TRACK_QUEUE_ITEM_2,
                                                              VIDEO_QUEUE_ITEM));
    }

    @Test
    public void shouldCreatePlayQueueWithPlayQueueItems() {
        assertTrackQueueItem(playQueue.getPlayQueueItem(0), TRACK_QUEUE_ITEM_1.getUrn());
        assertTrackQueueItem(playQueue.getPlayQueueItem(1), TRACK_QUEUE_ITEM_2.getUrn());
        assertVideoQueueItem(playQueue.getPlayQueueItem(2), VIDEO_QUEUE_ITEM.getAdData());
    }

    @Test
    public void shouldAddTrackToPlayQueue() {
        playQueue.addPlayQueueItem(TestPlayQueueItem.createTrack(Urn.forTrack(123L), "source3", "version3"));

        assertThat(playQueue.size()).isEqualTo(4);
        assertTrackQueueItem(playQueue.getPlayQueueItem(3), Urn.forTrack(123L), "source3", "version3");
    }

    @Test
    public void shouldHaveSeparateAdDataForEachItem() {
        assertThat(playQueue.getAdData(1)).isNotEqualTo(playQueue.getAdData(2));
    }

    @Test
    public void addAllPlayQueueItemsShouldAppendToPlayQueue() {
        playQueue = TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L, 3L), playSessionSource);
        playQueue.addAllPlayQueueItems(newArrayList(TRACK_QUEUE_ITEM_1, TRACK_QUEUE_ITEM_2, VIDEO_QUEUE_ITEM));

        assertThat(playQueue.size()).isEqualTo(6);
        assertTrackQueueItem(playQueue.getPlayQueueItem(3),
                             TRACK_QUEUE_ITEM_1.getUrn(),
                             TRACK_QUEUE_ITEM_1.getSource(),
                             TRACK_QUEUE_ITEM_1.getSourceVersion());
        assertTrackQueueItem(playQueue.getPlayQueueItem(4),
                             TRACK_QUEUE_ITEM_2.getUrn(),
                             TRACK_QUEUE_ITEM_2.getSource(),
                             TRACK_QUEUE_ITEM_2.getSourceVersion());
        assertVideoQueueItem(playQueue.getPlayQueueItem(5), VIDEO_QUEUE_ITEM.getAdData());
    }

    @Test
    public void indexOfReturnsMinusOneWhenTrackIsNotPresent() {
        assertThat(playQueue.indexOfTrackUrn(Urn.forTrack(4L))).isEqualTo(-1);
    }

    @Test
    public void indexOfReturnsIndexInQueueWhenTrackIsPresent() {
        assertThat(playQueue.indexOfTrackUrn(Urn.forTrack(2L))).isEqualTo(1);
    }

    @Test
    public void indexOfWithStartPositionReturnsMinusOneWhenTrackIsNotPresent() {
        assertThat(playQueue.indexOfTrackUrn(1, Urn.forTrack(4L))).isEqualTo(-1);
    }

    @Test
    public void indexOfWithStartPositionReturnsMinusOneWhenTrackIsNotPresentAfterGivenPosition() {
        assertThat(playQueue.indexOfTrackUrn(2, Urn.forTrack(2L))).isEqualTo(-1);
    }

    @Test
    public void indexOfWithStartPositionReturnsIndexInQueueWhenTrackIsPresent() {
        assertThat(playQueue.indexOfTrackUrn(1, Urn.forTrack(2L))).isEqualTo(1);
    }

    public void removeItem() {
        playQueue = PlayQueue.fromPlayQueueItems(Lists.<PlayQueueItem>newArrayList(TRACK_QUEUE_ITEM_1, TRACK_QUEUE_ITEM_2));

        playQueue.removeItem(TRACK_QUEUE_ITEM_1);

        final List<PlayQueueItem> expected = Collections.<PlayQueueItem>singletonList(TRACK_QUEUE_ITEM_2);
        assertThat(playQueue).isEqualTo(PlayQueue.fromPlayQueueItems(expected));
    }

    @Test
    public void removeItemIgnoreMissingItem() {
        playQueue = PlayQueue.fromPlayQueueItems(Lists.<PlayQueueItem>newArrayList(TRACK_QUEUE_ITEM_1));

        playQueue.removeItem(TRACK_QUEUE_ITEM_2);

        final List<PlayQueueItem> expected = Collections.<PlayQueueItem>singletonList(TRACK_QUEUE_ITEM_1);
        assertThat(playQueue).isEqualTo(PlayQueue.fromPlayQueueItems(expected));
    }

    @Test
    public void removesItemAtPosition() {
        playQueue.removeItemAtPosition(2);

        assertThat(playQueue).hasSize(2);
        assertTrackQueueItem(playQueue.getPlayQueueItem(0), Urn.forTrack(1L));
        assertTrackQueueItem(playQueue.getPlayQueueItem(1), Urn.forTrack(2L));
    }

    @Test
    public void shouldReportCorrectSize() {
        assertThat(playQueue.size()).isEqualTo(3);
    }

    @Test
    public void shouldReturnHasPreviousIfNotInFirstPosition() {
        assertThat(playQueue.hasPreviousItem(1)).isTrue();
    }

    @Test
    public void shouldReturnNoPreviousIfInFirstPosition() {
        assertThat(playQueue.hasPreviousItem(0)).isFalse();
    }

    @Test
    public void returnNoPreviousIfPQIsEmpty() {
        assertThat(PlayQueue.empty().hasPreviousItem(1)).isFalse();
    }

    @Test
    public void hasNextItemIsTrueIfNotAtEnd() {
        assertThat(playQueue.hasNextItem(0)).isTrue();
    }

    @Test
    public void hasNextItemIsFalseIfAtEnd() {
        assertThat(playQueue.hasNextItem(2)).isFalse();
    }

    @Test
    public void hasTrackAsNextItemIsTrueIfTrackNext() {
        assertThat(playQueue.hasTrackAsNextItem(0)).isTrue();
    }

    @Test
    public void hasTrackAsNextItemIsFalseIfTrackNotNext() {
        assertThat(playQueue.hasTrackAsNextItem(1)).isFalse();
    }

    @Test
    public void getUrnReturnsUrnForGivenPosition() throws Exception {
        assertThat(playQueue.getUrn(1)).isEqualTo(Urn.forTrack(2L));
    }

    @Test
    public void getUrnReturnsAdUrnForPositionWithVideo() throws Exception {
        assertThat(playQueue.getUrn(2)).isEqualTo(VIDEO_QUEUE_ITEM.getAdData().get().getAdUrn());
    }

    @Test
    public void getTrackItemUrnsReturnsListOfUrnsForTrackItems() throws Exception {
        assertThat(playQueue.getTrackItemUrns()).containsExactly(TRACK_QUEUE_ITEM_1.getUrn(),
                                                                 TRACK_QUEUE_ITEM_2.getUrn());
    }

    @Test
    public void playStationReturnsQueueWithStationPlayQueueItems() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final StationRecord station = StationFixtures.getStation(stationUrn);
        final List<StationTrack> tracks = station.getTracks();
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.STATIONS_INFO, Urn.forArtistStation(123L));
        PlayQueue playQueue = PlayQueue.fromStation(stationUrn, tracks, playSessionSource);

        assertThat(playQueue).hasSize(1);
        assertThat(playQueue.getTrackItemUrns()).containsExactly(tracks.get(0).getTrackUrn());

        final TrackQueueItem trackQueueItem = (TrackQueueItem) playQueue.getPlayQueueItem(0);
        assertThat(trackQueueItem.getSource()).isEqualTo("stations");
        assertThat(trackQueueItem.getSourceVersion()).isEqualTo("default");
        assertThat(trackQueueItem.getSourceUrn()).isEqualTo(stationUrn);
    }

    @Test
    public void hasSameTracksTrueWithSameTracksInSameOrder() {
        final PlayQueue playQueue2 = PlayQueue.fromPlayQueueItems(newArrayList(TRACK_QUEUE_ITEM_1,
                                                                               TRACK_QUEUE_ITEM_2,
                                                                               VIDEO_QUEUE_ITEM));
        assertThat(this.playQueue.hasSameTracks(playQueue2)).isTrue();
    }

    @Test
    public void hasSameTracksFalseWithSameTracksInDifferentOrder() {
        final PlayQueue playQueue2 = PlayQueue.fromPlayQueueItems(newArrayList(TRACK_QUEUE_ITEM_2,
                                                                               TRACK_QUEUE_ITEM_1,
                                                                               VIDEO_QUEUE_ITEM));
        assertThat(this.playQueue.hasSameTracks(playQueue2)).isFalse();
    }

    @Test
    public void itemsWithUrnReturnsAllItemsWithGivenUrn() {
        final Urn track1Urn = TRACK_QUEUE_ITEM_1.getUrn();
        final PlayQueueItem duplicateUrn = TestPlayQueueItem.createTrack(track1Urn, "source1", "version1");
        playQueue = PlayQueue.fromPlayQueueItems(newArrayList(TRACK_QUEUE_ITEM_1, TRACK_QUEUE_ITEM_2, duplicateUrn));

        assertThat(playQueue.itemsWithUrn(track1Urn)).containsExactly(TRACK_QUEUE_ITEM_1, duplicateUrn);
    }

    @Test
    public void getUrnsReturnsUrnsForGivenSection() {
        assertThat(playQueue.getItemUrns(1, 2)).containsExactly(TRACK_QUEUE_ITEM_2.getUrn(), VIDEO_QUEUE_ITEM.getUrn());
    }

    @Test
    public void replaceReplacesItemWithGivenItems() {
        final PlayQueueItem add1 = TestPlayQueueItem.createTrack(Urn.forTrack(1));
        final PlayQueueItem add2 = TestPlayQueueItem.createTrack(Urn.forTrack(2));
        playQueue.replaceItem(1, Arrays.asList(add1, add2));

        assertThat(playQueue.size()).isEqualTo(4);
        assertThat(playQueue.getPlayQueueItem(0)).isEqualTo(TRACK_QUEUE_ITEM_1);
        assertThat(playQueue.getPlayQueueItem(1)).isEqualTo(add1);
        assertThat(playQueue.getPlayQueueItem(2)).isEqualTo(add2);
        assertThat(playQueue.getPlayQueueItem(3)).isEqualTo(VIDEO_QUEUE_ITEM);
    }

    @Test
    public void playStationReturnsQueueWithStationPlayQueueItemsFromSuggestions() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final StationRecord station = StationFixtures.getStation(stationUrn);
        final List<StationTrack> tracks = station.getTracks();
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.STATIONS_INFO.get(), Urn.forArtistStation(123L), DiscoverySource.STATIONS_SUGGESTIONS);
        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn, tracks, playSessionSource);

        assertThat(playQueue).hasSize(1);
        assertThat(playQueue.getTrackItemUrns()).containsExactly(tracks.get(0).getTrackUrn());

        final TrackQueueItem trackQueueItem = (TrackQueueItem) playQueue.getPlayQueueItem(0);
        assertThat(trackQueueItem.getSource()).isEqualTo("stations:suggestions");
        assertThat(trackQueueItem.getSourceVersion()).isEqualTo("default");
        assertThat(trackQueueItem.getSourceUrn()).isEqualTo(stationUrn);
    }

    @Test
    public void shouldSwitchItems() {
        final List<Urn> allTracks = Arrays.asList(Urn.forTrack(1),
                                                  Urn.forTrack(2),
                                                  Urn.forTrack(3),
                                                  Urn.forTrack(4));
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(allTracks, PlaySessionSource.EMPTY, Collections.EMPTY_MAP);
        playQueue.moveItem(0, 1);
        assertThat(playQueue.getTrackItemUrns()).isEqualTo(Arrays.asList(Urn.forTrack(2),
                                                                         Urn.forTrack(1),
                                                                         Urn.forTrack(3),
                                                                         Urn.forTrack(4)));
    }

    @Test
    public void shouldSwitchItemsWhichAreNotNextToEachOther() {
        final List<Urn> allTracks = Arrays.asList(Urn.forTrack(1),
                                                  Urn.forTrack(2),
                                                  Urn.forTrack(3),
                                                  Urn.forTrack(4));
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(allTracks, PlaySessionSource.EMPTY, Collections.EMPTY_MAP);
        playQueue.moveItem(0, 2);
        assertThat(playQueue.getTrackItemUrns()).isEqualTo(Arrays.asList(Urn.forTrack(2),
                                                                         Urn.forTrack(3),
                                                                         Urn.forTrack(1),
                                                                         Urn.forTrack(4)));
    }


    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldThrowIndexOutOfBoundException() {
        final List<Urn> allTracks = Arrays.asList(Urn.forTrack(1), Urn.forTrack(2), Urn.forTrack(3), Urn.forTrack(4));
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(allTracks, PlaySessionSource.EMPTY, Collections.EMPTY_MAP);
        playQueue.moveItem(0, 5);
    }

    private void assertTrackQueueItem(PlayQueueItem playQueueItem, Urn trackUrn) {
        assertThat(playQueueItem.isTrack()).isTrue();
        assertThat(playQueueItem.getUrn()).isEqualTo(trackUrn);
    }

    private void assertTrackQueueItem(PlayQueueItem playQueueItem, Urn trackUrn, String source, String sourceVersion) {
        assertThat(playQueueItem.isTrack()).isTrue();
        final TrackQueueItem trackQueueItem = (TrackQueueItem) playQueueItem;
        assertThat(trackQueueItem.getUrn()).isEqualTo(trackUrn);
        assertThat(trackQueueItem.getSource()).isEqualTo(source);
        assertThat(trackQueueItem.getSourceVersion()).isEqualTo(sourceVersion);
    }

    private void assertVideoQueueItem(PlayQueueItem playQueueItem, Optional<AdData> adData) {
        assertThat(playQueueItem.isVideoAd()).isTrue();
        assertThat(playQueueItem.getAdData()).isEqualTo(adData);
    }

    private Map<Urn, Boolean> blockTracksMap(List<Urn> tracks) {
        Map<Urn, Boolean> map = new HashMap<>(tracks.size());
        for (Urn urn : tracks) {
            map.put(urn, true);
        }
        return map;
    }
}
