package com.soundcloud.android.collections;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.search.SearchAdapter;
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
            public int handleListItemClick(Context context, int position, long id, Screen screen) {
                return ItemClickResults.IGNORE;
            }
        };
    }


}
