package com.soundcloud.android.playback;

import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

@RunWith(MockitoJUnitRunner.class)
public class PlayQueueTest {

    private static final TrackQueueItem TRACK_QUEUE_ITEM_1 = TestPlayQueueItem.createTrack(Urn.forTrack(1L), "source1", "version1");
    private static final TrackQueueItem TRACK_QUEUE_ITEM_2 = TestPlayQueueItem.createTrack(Urn.forTrack(2L), "source2", "version2");
    private static final VideoQueueItem VIDEO_QUEUE_ITEM = TestPlayQueueItem.createVideo(PropertySet.create());

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
        assertVideoQueueItem(playQueue.getPlayQueueItem(2), VIDEO_QUEUE_ITEM.getMetaData());
    }

    @Test
    public void shouldAddTrackToPlayQueue() {
        playQueue.addPlayQueueItem(TestPlayQueueItem.createTrack(Urn.forTrack(123L), "source3", "version3"));

        assertThat(playQueue.size()).isEqualTo(4);
        assertTrackQueueItem(playQueue.getPlayQueueItem(3), Urn.forTrack(123L), "source3", "version3");
    }

    @Test
    public void shouldHaveSeparateMetaDataByDefaultForEachPlayQueueItem() {
        assertThat(playQueue.getMetaData(0)).isNotSameAs(playQueue.getMetaData(1));
    }

    @Test
    public void addAllPlayQueueItemsShouldAppendToPlayQueue() {
        playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playSessionSource);
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
        assertVideoQueueItem(playQueue.getPlayQueueItem(5), VIDEO_QUEUE_ITEM.getMetaData());
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
    public void insertsTrackAtPosition() throws CreateModelException {
        final Urn trackUrn = Urn.forTrack(123L);
        final PropertySet metaData = PropertySet.create();
        playQueue.insertTrack(1, trackUrn, metaData, true);

        assertTrackQueueItem(playQueue.getPlayQueueItem(1), trackUrn);
        assertThat(playQueue.getMetaData(1)).isEqualTo(metaData);

        assertThat(playQueue).hasSize(4);
        assertTrackQueueItem(playQueue.getPlayQueueItem(0), Urn.forTrack(1L));
        assertTrackQueueItem(playQueue.getPlayQueueItem(2), Urn.forTrack(2L));
    }

    @Test
    public void insertsVideoAtPosition() throws CreateModelException {
        final PropertySet metaData = PropertySet.create();
        playQueue.insertVideo(1, metaData);

        assertThat(playQueue.getPlayQueueItem(1).isVideo()).isTrue();
        assertThat(playQueue.getMetaData(1)).isEqualTo(metaData);

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
        final List<Urn> tracks = station.getTracks();
        PlayQueue playQueue = PlayQueue.fromStation(stationUrn, tracks);

        assertThat(playQueue).hasSize(1);
        assertThat(playQueue.getTrackItemUrns()).containsExactly(tracks.get(0));

        final TrackQueueItem trackQueueItem = (TrackQueueItem) playQueue.getPlayQueueItem(0);
        assertThat(trackQueueItem.getSource()).isEqualTo("stations");
        assertThat(trackQueueItem.getSourceVersion()).isEqualTo("default");
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

    private void assertVideoQueueItem(PlayQueueItem playQueueItem, PropertySet metadata) {
        assertThat(playQueueItem.isVideo()).isTrue();
        assertThat(playQueueItem.getMetaData()).isEqualTo(metadata);
    }
}
