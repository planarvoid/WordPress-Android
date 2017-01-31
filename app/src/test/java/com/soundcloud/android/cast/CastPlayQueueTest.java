package com.soundcloud.android.cast;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class CastPlayQueueTest {

    private static final Urn TRACK_URN1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(TRACK_URN1, TRACK_URN2);

    private CastPlayQueue castPlayQueue;

    @Before
    public void setUp() {
        castPlayQueue = new CastPlayQueue(TRACK_URN1, PLAY_QUEUE);
    }

    @Test
    public void constructorDoesNotSetRevision() {
        assertThat(castPlayQueue.getRevision()).isNull();
    }

    @Test
    public void specialRevisionConstructorWillSetTheRevision() {
        String revision = "rev";
        CastPlayQueue revisionedCastPlayQueue = new CastPlayQueue(revision, TRACK_URN1, PLAY_QUEUE);

        assertThat(revisionedCastPlayQueue.getRevision()).isEqualTo(revision);
    }

    @Test
    public void constructorSetsCurrentIndexCorrectlyForCurrentPlayingTrack() {
        CastPlayQueue castPlayQueue = new CastPlayQueue(TRACK_URN2, PLAY_QUEUE);

        assertThat(castPlayQueue.getCurrentIndex()).isEqualTo(1);
    }

    @Test
    public void getCurrentTrackUrnReturnsTheUrnOfTheCurrentPlayingTrack() {
        assertThat(castPlayQueue.getCurrentTrackUrn()).isEqualTo(TRACK_URN1);
    }

    @Test
    public void tryingToGetCurrentTrackUrnForTrackNotPresentInQueueWillReturnNotSet() {
        CastPlayQueue castPlayQueue = new CastPlayQueue(Urn.forTrack(789L), PLAY_QUEUE);

        assertThat(castPlayQueue.getCurrentTrackUrn()).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void getQueueUrnsTransformsRemoteTracksCorrectly() {
        List<Urn> queueUrns = castPlayQueue.getQueueUrns();

        assertThat(queueUrns).isEqualTo(PLAY_QUEUE);
    }

    @Test
    public void hasSameTracksReturnsTrueForSameUrnsInQueue() {
        assertThat(castPlayQueue.hasSameTracks(Arrays.asList(TRACK_URN1, TRACK_URN2))).isTrue();
    }

    @Test
    public void hasSameTracksReturnsFalseIfQueueIsDifferentButAllTracksAreContained() {
        assertThat(castPlayQueue.hasSameTracks(Arrays.asList(TRACK_URN1, TRACK_URN2, Urn.forTrack(789L)))).isFalse();
    }

    @Test
    public void hasSameTracksReturnsFalseIfQueueIsDifferentAndAtLeastOneOfTheTracksIsNotContained() {
        assertThat(castPlayQueue.hasSameTracks(Arrays.asList(TRACK_URN1, Urn.forTrack(789L), Urn.forTrack(1654L)))).isFalse();
    }

    @Test
    public void hasSameTracksReturnsFalseIfHasEmptyQueue() {
        CastPlayQueue castPlayQueue = new CastPlayQueue(null, emptyList());

        assertThat(castPlayQueue.hasSameTracks(Arrays.asList(TRACK_URN1, TRACK_URN2))).isFalse();
    }

    @Test
    public void hasSameTracksReturnsFalseForNullParameter() {
        assertThat(castPlayQueue.hasSameTracks(null)).isFalse();
    }

    @Test
    public void isEmptyReturnsTrueForEmptyUrnList() {
        assertThat(new CastPlayQueue(TRACK_URN1, emptyList()).isEmpty()).isTrue();
    }
}