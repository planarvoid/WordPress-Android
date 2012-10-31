package com.soundcloud.android.adapter;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.view.adapter.LazyRow;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class SearchAdapterTest {

    @Test
    public void shouldCreateAdapter() throws Exception {
        SearchAdapter adapter = new SearchAdapter(DefaultTestRunner.application, Content.SEARCH.uri) {
            @Override
            protected LazyRow createRow(int position) {
                return null;
            }

            @Override
            public void handleListItemClick(int position, long id) {
                return position;
            }
        };
    }


}
