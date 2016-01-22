package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchUpsellItem.UPSELL_URN;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchUpsellItemTest {

    private SearchUpsellItem searchUpsellItem;

    @Before
    public void setUp() {
        searchUpsellItem = new SearchUpsellItem();
    }

    @Test
    public void shouldHaveCorrectUrn() {
        assertThat(searchUpsellItem.getEntityUrn()).isEqualTo(UPSELL_URN);
    }

}
