package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class PlaybackStateTransitionTest extends AndroidUnitTest {

    @Test
    public void isForTrackIsFalseWithDifferentTrackUrn() {
        final PlaybackStateTransition stateTransition =
                new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, Urn.forTrack(2L), 0, 0);
        assertThat(stateTransition.isForUrn(Urn.forTrack(1L))).isFalse();
    }

    @Test
    public void isForTrackIsTrueWithSameTrackUrn() {
        final PlaybackStateTransition stateTransition =
                new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, Urn.forTrack(1L), 0, 0);
        assertThat(stateTransition.isForUrn(Urn.forTrack(1L))).isTrue();
    }

    @Test
    public void implementsEqualsContract() {
        EqualsVerifier.forClass(PlaybackStateTransition.class)
                      .withOnlyTheseFields("newState", "reason", "progress", "itemUrn")
                      .usingGetClass()
                      .verify();
    }
}
