package com.soundcloud.android.home;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DiscoveryCardMapperTest {
    @Test
    public void mapsSingletonSelectionCard() throws Exception {
        final DiscoveryCard card = DiscoveryCardMapper.map(ApiDiscoveryCardTest.EXPECTED_SINGLETON_SELECTION_CARD);

        assertThat(card.kind()).isEqualTo(DiscoveryCard.Kind.SINGLETON_SELECTION_CARD);
    }

    @Test
    public void mapsSelectionCard() throws Exception {
        final DiscoveryCard card = DiscoveryCardMapper.map(ApiDiscoveryCardTest.EXPECTED_SELECTION_CARD);

        assertThat(card.kind()).isEqualTo(DiscoveryCard.Kind.SELECTION_CARD);
    }
}
