package com.soundcloud.android.discovery;


import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.search.SearchItemRenderer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DiscoveryAdapterTest extends AndroidUnitTest {

    @Mock SearchItemRenderer searchItemRenderer;

    private DiscoveryAdapter adapter;
    private DiscoveryCard searchItem = DiscoveryCard.forSearchItem();

    @Before
    public void setUp() {
        adapter = new DiscoveryAdapter(searchItemRenderer);
    }

    @Test
    public void rendersCorrectViewTypes() {
        adapter.onNext(asList(searchItem));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(DiscoveryCard.Kind.SEARCH_ITEM.ordinal());
    }

    @Test
    public void setsSearchListener() {
        final SearchItemRenderer.SearchListener searchListener = mock(SearchItemRenderer.SearchListener.class);

        adapter.setSearchListener(searchListener);

        verify(searchItemRenderer).setSearchListener(searchListener);
    }
}
