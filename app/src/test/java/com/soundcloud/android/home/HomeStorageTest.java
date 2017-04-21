package com.soundcloud.android.home;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ModelCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@RunWith(JUnit4.class)
public class HomeStorageTest {
    private HomeStorage homeStorage;

    @Before
    public void setUp() throws Exception {
        homeStorage = new HomeStorage();
    }

    @Test
    public void filtersOutInvalidCard() throws Exception {
        ApiHomeCard invalidCard = ApiHomeCard.create(null, null);
        ApiHomeCard validCard = ApiHomeCard.create(null, ApiSelectionCardTest.EXPECTED_SELECTION_CARD);

        homeStorage.store(new ModelCollection<>(Lists.newArrayList(invalidCard, validCard)));

        final List<ApiHomeCard> apiHomeCards = homeStorage.homeCards().test().assertValueCount(1).values().get(0);

        assertThat(apiHomeCards).containsExactly(validCard);
    }
}
