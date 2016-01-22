package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class SearchPremiumItemTest {

    private static final int RESULTS_COUNT = 10;

    private SearchPremiumItem searchPremiumItem;

    @Before
    public void setUp() {
        searchPremiumItem = new SearchPremiumItem(Collections.<PropertySet>emptyList(), Optional.<Link>absent(), RESULTS_COUNT);
    }

    @Test
    public void shouldNotHaveUrn() {
        assertThat(searchPremiumItem.getEntityUrn()).isEqualTo(Urn.NOT_SET);
    }
}
