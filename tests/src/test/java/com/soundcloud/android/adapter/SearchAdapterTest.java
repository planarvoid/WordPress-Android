package com.soundcloud.android.adapter;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.view.adapter.IconLayout;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;

@RunWith(DefaultTestRunner.class)
public class SearchAdapterTest {

    @Test
    public void shouldCreateAdapter() throws Exception {
        SearchAdapter adapter = new SearchAdapter(Content.SEARCH.uri) {
            @Override
            protected IconLayout createRow(Context context, int position) {
                return null;
            }

            @Override
            public int handleListItemClick(Context context, int position, long id) {
                return ItemClickResults.IGNORE;
            }
        };
    }


}
