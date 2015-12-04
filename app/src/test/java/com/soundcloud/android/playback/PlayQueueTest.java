package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
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
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class PlayQueueTest extends AndroidUnitTest {

    private static final TrackQueueItem TRACK_QUEUE_ITEM_1 = TestPlayQueueItem.createTrack(Urn.forTrack(1L), "source1", "version1");
    private static final TrackQueueItem TRACK_QUEUE_ITEM_2 = TestPlayQueueItem.createTrack(Urn.forTrack(2L), "source2", "version2");
    private static final VideoQueueItem VIDEO_QUEUE_ITEM = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(722L)));

    private static final int PLAY_QUEUE_ITEM_COUNT = 3;

    private PlayQueue playQueue = PlayQueue.empty();
    private PlaySessionSource playSessionSource;

    @Before
    public void setUp() throws Exception {
        playSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS, Urn.forPlaylist(123), Urn.forUser(456), PLAY_QUEUE_ITEM_COUNT);
        playQueue = new PlayQueue(newArrayList(TRACK_QUEUE_ITEM_1, TRACK_QUEUE_ITEM_2, VIDEO_QUEUE_ITEM));
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
                TRACK_QUEUE_ITEM_1.getTrackUrn(),
                TRACK_QUEUE_ITEM_1.getSource(),
                TRACK_QUEUE_ITEM_1.getSourceVersion());
        assertTrackQueueItem(playQueue.getPlayQueueItem(4),
                TRACK_QUEUE_ITEM_2.getTrackUrn(),
                TRACK_QUEUE_ITEM_2.getSource(),
                TRACK_QUEUE_ITEM_2.getSourceVersion());
        assertVideoQueueItem(playQueue.getPlayQueueItem(5), VIDEO_QUEUE_ITEM.getAdData());
    }

    @Test
    public void indexOfReturnsMinusOneWhenTrackIsNotPresent() {
        assertThat(playQueue.indexOfTrackUrn(Urn.forTrack(4l))).isEqualTo(-1);
    }

    @Test
    public void indexOfReturnsIndexInQueueWhenTrackIsPresent() {
        assertThat(playQueue.indexOfTrackUrn(Urn.forTrack(2l))).isEqualTo(1);
    }

    @Test
    public void indexOfWithStartPositionReturnsMinusOneWhenTrackIsNotPresent() {
        assertThat(playQueue.indexOfTrackUrn(1, Urn.forTrack(4l))).isEqualTo(-1);
    }

    @Test
    public void indexOfWithStartPositionReturnsMinusOneWhenTrackIsNotPresentAfterGivenPosition() {
        assertThat(playQueue.indexOfTrackUrn(2, Urn.forTrack(2l))).isEqualTo(-1);
    }

    @Test
    public void indexOfWithStartPositionReturnsIndexInQueueWhenTrackIsPresent() {
        assertThat(playQueue.indexOfTrackUrn(1, Urn.forTrack(2l))).isEqualTo(1);
    }

    @Test
    public void insertsAudioAdAtPosition() throws CreateModelException {
        final Urn trackUrn = Urn.forTrack(123L);
        final AudioAd adData = AdFixtures.getAudioAd(Urn.forTrack(123L));
        playQueue.insertAudioAd(1, trackUrn, adData, true);

        assertTrackQueueItem(playQueue.getPlayQueueItem(1), trackUrn);
        assertThat(playQueue.getAdData(1)).isEqualTo(Optional.of(adData));

        assertThat(playQueue).hasSize(4);
        assertTrackQueueItem(playQueue.getPlayQueueItem(0), Urn.forTrack(1L));
        assertTrackQueueItem(playQueue.getPlayQueueItem(2), Urn.forTrack(2L));
    }

    @Test
    public void insertsVideoAtPosition() throws CreateModelException {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        playQueue.insertVideo(1, videoAd);

        assertThat(playQueue.getPlayQueueItem(1).isVideo()).isTrue();
        assertThat(playQueue.getAdData(1)).isEqualTo(Optional.of(videoAd));

        assertThat(playQueue).hasSize(4);
        assertTrackQueueItem(playQueue.getPlayQueueItem(0), Urn.forTrack(1L));
        assertTrackQueueItem(playQueue.getPlayQueueItem(2), Urn.forTrack(2L));
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
    public void getUrnReturnsUrnForGivenPosition() throws Exception {
        assertThat(playQueue.getUrn(1)).isEqualTo(Urn.forTrack(2L));
    }

    @Test
    public void getUrnReturnsNotSetForPositionWithVideo() throws Exception {
        assertThat(playQueue.getUrn(2)).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void getTrackItemUrnsReturnsListOfUrnsForTrackItems() throws Exception {
        assertThat(playQueue.getTrackItemUrns()).containsExactly(TRACK_QUEUE_ITEM_1.getTrackUrn(), TRACK_QUEUE_ITEM_2.getTrackUrn());
    }

    @Test
    public void playStationReturnsQueueWithStationPlayQueueItems() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final StationRecord station = StationFixtures.getStation(stationUrn);
        final List<StationTrack> tracks = station.getTracks();
        PlayQueue playQueue = PlayQueue.fromStation(stationUrn, tracks);

        assertThat(playQueue).hasSize(1);
        assertThat(playQueue.getTrackItemUrns()).containsExactly(tracks.get(0).getTrackUrn());

        final TrackQueueItem trackQueueItem = (TrackQueueItem) playQueue.getPlayQueueItem(0);
        assertThat(trackQueueItem.getSource()).isEqualTo("stations");
        assertThat(trackQueueItem.getSourceVersion()).isEqualTo("default");
        assertThat(trackQueueItem.getSourceUrn()).isEqualTo(stationUrn);
    }

    private void assertTrackQueueItem(PlayQueueItem playQueueItem, Urn trackUrn) {
        assertThat(playQueueItem.isTrack()).isTrue();
        assertThat(playQueueItem.getUrn()).isEqualTo(trackUrn);
    }

    private void assertTrackQueueItem(PlayQueueItem playQueueItem, Urn trackUrn, String source, String sourceVersion) {
        assertThat(playQueueItem.isTrack()).isTrue();
        final TrackQueueItem trackQueueItem = (TrackQueueItem) playQueueItem;
        assertThat(trackQueueItem.getTrackUrn()).isEqualTo(trackUrn);
        assertThat(trackQueueItem.getSource()).isEqualTo(source);
        assertThat(trackQueueItem.getSourceVersion()).isEqualTo(sourceVersion);
    }

    private void assertVideoQueueItem(PlayQueueItem playQueueItem, Optional<AdData> adData) {
        assertThat(playQueueItem.isVideo()).isTrue();
        assertThat(playQueueItem.getAdData()).isEqualTo(adData);
    }
}
