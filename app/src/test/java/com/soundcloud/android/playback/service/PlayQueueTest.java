package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.createTracksUrn;

import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueTest {

    private static final String ORIGIN_PAGE = "explore:music:techno";
    private static final PlayQueueItem PLAY_QUEUE_ITEM_1 = PlayQueueItem.fromTrack(Urn.forTrack(1L), "source1", "version1");
    private static final PlayQueueItem PLAY_QUEUE_ITEM_2 = PlayQueueItem.fromTrack(Urn.forTrack(2L), "source2", "version2");

    private PlaySessionSource playSessionSource;
    private PublicApiPlaylist playlist;

    @Before
    public void setUp() throws Exception {
        playlist = ModelFixtures.create(PublicApiPlaylist.class);
        playSessionSource  = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playSessionSource.setExploreVersion("1.0");
    }

    @Test
    public void shouldCreatePlayQueueWithItems() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2));
        expect(playQueue.getUrn(0)).toEqual(PLAY_QUEUE_ITEM_1.getTrackUrn());
        expect(playQueue.getUrn(1)).toEqual(PLAY_QUEUE_ITEM_2.getTrackUrn());
    }

    @Test
    public void shouldAddTrackToPlayQueue() {
        PlayQueue playQueue = createPlayQueue(createTracksUrn(1L, 2L, 3L), playSessionSource);

        playQueue.addTrack(Urn.forTrack(123L), "source3", "version3");

        expect(playQueue.size()).toEqual(4);
        expect(playQueue.getTrackId(3)).toEqual(123L);
        expect(playQueue.getTrackSource(3)).toEqual("source3");
        expect(playQueue.getSourceVersion(3)).toEqual("version3");
    }

    @Test
    public void insertsTrackAtPosition() throws CreateModelException {
        PlayQueue playQueue = createPlayQueue(createTracksUrn(1L, 2L, 3L), playSessionSource);

        TrackUrn trackUrn = Urn.forTrack(123L);
        PropertySet metaData = PropertySet.create();
        playQueue.insertTrack(1, trackUrn, metaData, true);

        expect(playQueue.getUrn(1)).toEqual(trackUrn);
        expect(playQueue.getMetaData(1)).toEqual(metaData);

        expect(playQueue.size()).toBe(4);
        expect(playQueue.getUrn(0)).toEqual(Urn.forTrack(1L));
        expect(playQueue.getUrn(2)).toEqual(Urn.forTrack(2L));
    }

    @Test
    public void shouldRemoveAtPosition() {
        PlayQueue playQueue = createPlayQueue(createTracksUrn(1L, 2L, 3L), playSessionSource);

        playQueue.remove(1);

        expect(playQueue.getUrn(1)).toEqual(TrackUrn.forTrack(3L));
        expect(playQueue.size()).toBe(2);
    }

    @Test
    public void shouldReportCorrectSize() {
        PlayQueue playQueue = createPlayQueue(createTracksUrn(1L, 2L, 3L), playSessionSource);
        expect(playQueue.size()).toEqual(3);
    }

    @Test
    public void shouldReturnHasPreviousIfNotInFirstPosition() {
        PlayQueue playQueue = createPlayQueue(createTracksUrn(1L, 2L));

        expect(playQueue.hasPreviousTrack(1)).toBeTrue();
    }

    @Test
    public void shouldReturnNoPreviousIfInFirstPosition() {
        PlayQueue playQueue = createPlayQueue(createTracksUrn(1L, 2L));

        expect(playQueue.hasPreviousTrack(0)).toBeFalse();
    }

    @Test
    public void hasNextTrackIsTrueIfNotAtEnd() {
        expect(createPlayQueue(createTracksUrn(1L, 2L)).hasNextTrack(0)).toBeTrue();
    }

    @Test
    public void hasNextTrackIsFalseIfAtEnd() {
        expect(createPlayQueue(createTracksUrn(1L, 2L)).hasNextTrack(1)).toBeFalse();
    }

    @Test
    public void getUrnReturnsUrnForGivenPosition() throws Exception {
        PlayQueue playQueue = createPlayQueue(createTracksUrn(1L, 2L, 3L));
        expect(playQueue.getUrn(1)).toEqual(Urn.forTrack(2L));
    }

    @Test
    public void getUrnReturnsNotSetUrnWithEmptyQueue() throws Exception {
        expect(PlayQueue.empty().getUrn(0)).toEqual(Urn.forTrack(Consts.NOT_SET));
    }

    @Test
    public void getUrnAtPositionReturnsNotSetForEmptyQueue() throws Exception {
        expect(PlayQueue.empty().getUrn(0)).toBe(TrackUrn.NOT_SET);
    }

    @Test
    public void getUrnAtInvalidPositionReturnsNotSet() throws Exception {
        PlayQueue playQueue = createPlayQueue(createTracksUrn(1L, 2L, 3L));
        expect(playQueue.getUrn(-1)).toBe(TrackUrn.NOT_SET);
        expect(playQueue.getUrn(3)).toBe(TrackUrn.NOT_SET);
    }

    @Test
    public void getUrnAtPositionReturnsUrnAtPosition() throws Exception {
        PlayQueue playQueue = createPlayQueue(createTracksUrn(1L, 2L, 3L));
        expect(playQueue.getUrn(2)).toEqual(Urn.forTrack(3));
    }

    private PlayQueue createPlayQueue(List<TrackUrn> trackUrns, PlaySessionSource source) {
        return PlayQueue.fromTrackUrnList(trackUrns, source);
    }

    private PlayQueue createPlayQueue(List<TrackUrn> trackUrns) {
        return createPlayQueue(trackUrns, PlaySessionSource.EMPTY);
    }

}
