package com.soundcloud.android.playback;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.Station;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PlayQueueTest {

    private static final PlayQueueItem PLAY_QUEUE_ITEM_1 = new PlayQueueItem.Builder(Urn.forTrack(1L))
            .fromSource("source1", "version1")
            .build();
    private static final PlayQueueItem PLAY_QUEUE_ITEM_2 = new PlayQueueItem.Builder(Urn.forTrack(2L))
            .fromSource("source2", "version2")
            .build();
    private static final int PLAY_QUEUE_ITEM_COUNT = 2;

    private PlaySessionSource playSessionSource;

    @Before
    public void setUp() throws Exception {
        playSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS, Urn.forPlaylist(123), Urn.forUser(456), PLAY_QUEUE_ITEM_COUNT);
    }

    @Test
    public void shouldCreatePlayQueueWithItems() {
        PlayQueue playQueue = new PlayQueue(asList(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2));
        assertThat(playQueue.getUrn(0)).isEqualTo(PLAY_QUEUE_ITEM_1.getTrackUrn());
        assertThat(playQueue.getUrn(1)).isEqualTo(PLAY_QUEUE_ITEM_2.getTrackUrn());
    }

    @Test
    public void shouldAddTrackToPlayQueue() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L), playSessionSource);
        playQueue.addPlayQueueItem(new PlayQueueItem.Builder(Urn.forTrack(123L)).fromSource("source3", "version3").build());

        assertThat(playQueue.size()).isEqualTo(4);
        assertThat(playQueue.getTrackId(3)).isEqualTo(123L);
        assertThat(playQueue.getTrackSource(3)).isEqualTo("source3");
        assertThat(playQueue.getSourceVersion(3)).isEqualTo("version3");
    }

    @Test
    public void shouldHaveSeparateMetaDataByDefaultForEachPlayQueueItem() {
        PlayQueue playQueue = new PlayQueue(asList(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2));
        assertThat(playQueue.getMetaData(0)).isNotSameAs(playQueue.getMetaData(1));
    }

    @Test
    public void addPlayQueueItemShouldAppendToPlayQueue() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L), playSessionSource);

        playQueue.addPlayQueueItem(new PlayQueueItem.Builder(Urn.forTrack(123L))
                .fromSource("source3", "version3")
                .build());

        assertThat(playQueue.size()).isEqualTo(4);
        assertThat(playQueue.getTrackId(3)).isEqualTo(123L);
        assertThat(playQueue.getTrackSource(3)).isEqualTo("source3");
        assertThat(playQueue.getSourceVersion(3)).isEqualTo("version3");
    }

    @Test
    public void addAllPlayQueueItemsShouldAppendToPlayQueue() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L), playSessionSource);

        playQueue.addAllPlayQueueItems(Arrays.asList(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2));

        assertThat(playQueue.size()).isEqualTo(5);
        assertThat(playQueue.getUrn(3)).isEqualTo(PLAY_QUEUE_ITEM_1.getTrackUrn());
        assertThat(playQueue.getTrackSource(3)).isEqualTo(PLAY_QUEUE_ITEM_1.getSource());
        assertThat(playQueue.getSourceVersion(3)).isEqualTo(PLAY_QUEUE_ITEM_1.getSourceVersion());

        assertThat(playQueue.getUrn(4)).isEqualTo(PLAY_QUEUE_ITEM_2.getTrackUrn());
        assertThat(playQueue.getTrackSource(4)).isEqualTo(PLAY_QUEUE_ITEM_2.getSource());
        assertThat(playQueue.getSourceVersion(4)).isEqualTo(PLAY_QUEUE_ITEM_2.getSourceVersion());
    }

    @Test
    public void indexOfReturnsMinusOneWhenTrackIsNotPresent() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L), playSessionSource);

        assertThat(playQueue.indexOf(Urn.forTrack(4l))).isEqualTo(-1);
    }

    @Test
    public void indexOfReturnsIndexInQueueWhenTrackIsPresent() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L), playSessionSource);

        assertThat(playQueue.indexOf(Urn.forTrack(2l))).isEqualTo(1);
    }

    @Test
    public void insertsTrackAtPosition() throws CreateModelException {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L), playSessionSource);

        Urn trackUrn = Urn.forTrack(123L);
        PropertySet metaData = PropertySet.create();
        playQueue.insertTrack(1, trackUrn, metaData, true);

        assertThat(playQueue.getUrn(1)).isEqualTo(trackUrn);
        assertThat(playQueue.getMetaData(1)).isEqualTo(metaData);

        assertThat(playQueue).hasSize(4);
        assertThat(playQueue.getUrn(0)).isEqualTo(Urn.forTrack(1L));
        assertThat(playQueue.getUrn(2)).isEqualTo(Urn.forTrack(2L));
    }

    @Test
    public void shouldRemoveAtPosition() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L), playSessionSource);

        playQueue.remove(1);

        assertThat(playQueue.getUrn(1)).isEqualTo(Urn.forTrack(3L));
        assertThat(playQueue).hasSize(2);
    }

    @Test
    public void shouldReportCorrectSize() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L), playSessionSource);
        assertThat(playQueue.size()).isEqualTo(3);
    }

    @Test
    public void shouldReturnHasPreviousIfNotInFirstPosition() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L));

        assertThat(playQueue.hasPreviousTrack(1)).isTrue();
    }

    @Test
    public void shouldReturnNoPreviousIfInFirstPosition() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L));

        assertThat(playQueue.hasPreviousTrack(0)).isFalse();
    }

    @Test
    public void returnNoPreviousIfPQIsEmpty() {
        PlayQueue playQueue = PlayQueue.empty();

        assertThat(playQueue.hasPreviousTrack(1)).isFalse();
    }

    @Test
    public void hasNextTrackIsTrueIfNotAtEnd() {
        assertThat(createPlayQueue(TestUrns.createTrackUrns(1L, 2L)).hasNextTrack(0)).isTrue();
    }

    @Test
    public void hasNextTrackIsFalseIfAtEnd() {
        assertThat(createPlayQueue(TestUrns.createTrackUrns(1L, 2L)).hasNextTrack(1)).isFalse();
    }

    @Test
    public void getUrnReturnsUrnForGivenPosition() throws Exception {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        assertThat(playQueue.getUrn(1)).isEqualTo(Urn.forTrack(2L));
    }

    @Test
    public void getUrnReturnsNotSetUrnWithEmptyQueue() throws Exception {
        assertThat(PlayQueue.empty().getUrn(0)).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void getUrnAtPositionReturnsNotSetForEmptyQueue() throws Exception {
        assertThat(PlayQueue.empty().getUrn(0)).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void getUrnAtInvalidPositionReturnsNotSet() throws Exception {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        assertThat(playQueue.getUrn(-1)).isEqualTo(Urn.NOT_SET);
        assertThat(playQueue.getUrn(3)).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void getUrnAtPositionReturnsUrnAtPosition() throws Exception {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        assertThat(playQueue.getUrn(2)).isEqualTo(Urn.forTrack(3));
    }

    @Test
    public void playStationReturnsQueueWithStationPlayQueueItems() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Station station = StationFixtures.getStation(stationUrn);
        final List<Urn> tracks = station.getTracks();
        PlayQueue playQueue = PlayQueue.fromStation(stationUrn, tracks);

        assertThat(playQueue).hasSize(1);
        assertThat(playQueue.getTrackUrns()).containsExactly(tracks.get(0));
        assertThat(playQueue.getTrackSource(0)).isEqualTo("stations");
        assertThat(playQueue.getSourceVersion(0)).isEqualTo("default");
    }

    private PlayQueue createPlayQueue(List<Urn> trackUrns, PlaySessionSource source) {
        return PlayQueue.fromTrackUrnList(trackUrns, source);
    }

    private PlayQueue createPlayQueue(List<Urn> trackUrns) {
        return createPlayQueue(trackUrns, PlaySessionSource.EMPTY);
    }

}
