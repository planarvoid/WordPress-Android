package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ModelCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@RunWith(JUnit4.class)
public class DiscoveryStorageTest {
    private DiscoveryStorage discoveryStorage;

    @Before
    public void setUp() throws Exception {
        discoveryStorage = new DiscoveryStorage();
    }

    @Test
    public void filtersOutInvalidCard() throws Exception {
        ApiDiscoveryCard invalidCard = ApiDiscoveryCard.create(null, null);
        ApiDiscoveryCard validCard = ApiDiscoveryCard.create(null, ApiSelectionCardTest.EXPECTED_SELECTION_CARD);

        discoveryStorage.store(new ModelCollection<>(Lists.newArrayList(invalidCard, validCard)));

        final List<ApiDiscoveryCard> apiDiscoveryCards = discoveryStorage.discoveryCards().test().assertValueCount(1).values().get(0);

        assertThat(apiDiscoveryCards).containsExactly(validCard);
    }
}
