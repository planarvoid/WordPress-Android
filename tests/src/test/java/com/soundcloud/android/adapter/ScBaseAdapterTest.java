package com.soundcloud.android.adapter;

import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.view.adapter.LazyRow;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class ScBaseAdapterTest {

    @Test
    public void shouldCreateAdapter() throws Exception {
        ScBaseAdapter<User> adapter = new ScBaseAdapter<User>(DefaultTestRunner.application, Content.USER.uri) {
            @Override
            protected LazyRow createRow(int position) {
                return null;
            }
        };
    }
}
