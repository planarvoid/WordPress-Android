package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlaybackStateTransitionTest {

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
