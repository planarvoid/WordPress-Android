package com.soundcloud.android.cast;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CastQueueSlicerTest {

    private static final int TEST_QUEUE_SIZE = 8;
    private static final int MAX_NUMBER_OF_TRACKS_CASTED_AT_ONCE = 5;
    private static final int NUMBER_OF_TRACKS_TO_KEEP_BEFORE_PIVOT = 2;

    private CastQueueSlicer slicer;

    @Before
    public void setUp() {
        this.slicer = new CastQueueSlicer();
    }

    private PlayQueue slice(List<Urn> urns, int pivotPosition) {
        return slicer.slice(urns, pivotPosition, MAX_NUMBER_OF_TRACKS_CASTED_AT_ONCE, NUMBER_OF_TRACKS_TO_KEEP_BEFORE_PIVOT);
    }

    @Test
    public void notSliceSmallerThanLimitPlayQueues() {
        List<Urn> urns = Arrays.asList(Urn.forTrack(123), Urn.forTrack(456));

        PlayQueue sliced = slice(urns, 1);

        assertThat(sliced.getTrackItemUrns()).isEqualTo(urns);
    }

    @Test
    public void keepSlicedQueueSizeToTheLimit() {
        List<Urn> urns = randomUrns(MAX_NUMBER_OF_TRACKS_CASTED_AT_ONCE + 1);

        PlayQueue sliced = slice(urns, 0);

        assertThat(sliced.getTrackItemUrns().size()).isEqualTo(MAX_NUMBER_OF_TRACKS_CASTED_AT_ONCE);
    }

    @Test
    public void pivotKeepsItsPositionIfFirstOnTheList() {
        List<Urn> urns = randomUrns(TEST_QUEUE_SIZE);
        int pivot = 0;
        Urn pivotUrn = urns.get(pivot);

        PlayQueue slice = slice(urns, pivot);

        assertThat(slice.getTrackItemUrns().get(0)).isEqualTo(pivotUrn);
        assertThat(slice.getTrackItemUrns()).isEqualTo(urns.subList(0, 5));
    }

    @Test
    public void pivotKeepsItsPositionIfIntoTheWindow() {
        List<Urn> urns = randomUrns(TEST_QUEUE_SIZE);
        int pivot = 1;
        Urn pivotUrn = urns.get(pivot);

        PlayQueue slice = slice(urns, pivot);

        assertThat(slice.getTrackItemUrns().get(1)).isEqualTo(pivotUrn);
        assertThat(slice.getTrackItemUrns()).isEqualTo(urns.subList(0, 5));
    }

    @Test
    public void subListToFormTheWindowAroundThePivotWhileKeepingSomeHistoryBeforeIt() {
        List<Urn> urns = randomUrns(TEST_QUEUE_SIZE);
        int pivot = 2;
        Urn pivotUrn = urns.get(pivot);

        PlayQueue slice = slice(urns, pivot);

        assertThat(slice.getTrackItemUrns().get(2)).isEqualTo(pivotUrn);
        assertThat(slice.getTrackItemUrns()).isEqualTo(urns.subList(0, 5));
    }

    @Test
    public void subListByMovingTheWindowAroundThePivotWhileKeepingSomeHistoryBeforeIt() {
        List<Urn> urns = randomUrns(TEST_QUEUE_SIZE);
        int pivot = 3;
        Urn pivotUrn = urns.get(pivot);

        PlayQueue slice = slice(urns, pivot);

        assertThat(slice.getTrackItemUrns().get(2)).isEqualTo(pivotUrn);
        assertThat(slice.getTrackItemUrns()).isEqualTo(urns.subList(1, 6));
    }

    @Test
    public void moveTheWindowBackwardsIfPivotIsCloseToTheEndOfTheList() {
        List<Urn> urns = randomUrns(TEST_QUEUE_SIZE);
        int pivot = 6;
        Urn pivotUrn = urns.get(pivot);

        PlayQueue slice = slice(urns, pivot);

        assertThat(slice.getTrackItemUrns().get(3)).isEqualTo(pivotUrn);
        assertThat(slice.getTrackItemUrns()).isEqualTo(urns.subList(3, urns.size()));
    }

    private List<Urn> randomUrns(int size) {
        Random random = new Random();
        List<Urn> urns = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            urns.add(Urn.forTrack(Math.abs(random.nextInt())));
        }
        return urns;
    }

}
