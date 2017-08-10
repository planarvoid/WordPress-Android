package com.soundcloud.android.ads;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class AdStateProviderTest {

    private static final String UUID = "111-1111-111";
    private static final VideoAd VIDEO_AD = AdFixtures.getVideoAd(Urn.forAd("adserver", "123"));
    private static final AdPlayStateTransition TRANSITION = AdPlayStateTransition.create(VIDEO_AD, TestPlayerTransitions.playing(), false, new Date(999));

    private AdStateProvider stateProvider;

    @Before
    public void setUp() {
        stateProvider = new AdStateProvider();
    }

    @Test
    public void getReturnsInlayAdTransitionIfItWasInsertedForUUID() {
        stateProvider.put(UUID, TRANSITION);

        assertThat(stateProvider.get(UUID)).isEqualTo(Optional.of(TRANSITION));
    }

    @Test
    public void getReturnsAbsentIfTransitionForUUIDWasNeverInserted() {
        assertThat(stateProvider.get(UUID)).isEqualTo(Optional.absent());
    }

    @Test
    public void removeRemovesTransitionForUUID() {
        stateProvider.put(UUID, TRANSITION);
        stateProvider.remove(UUID);

        assertThat(stateProvider.get(UUID)).isEqualTo(Optional.absent());
    }
}
