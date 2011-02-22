package com.soundcloud.android.task;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.User;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;


@RunWith(RobolectricTestRunner.class)
public class LoadCollectionTaskTests {

    @Test
    public void shouldLoadUsers() throws Exception {

        LoadCollectionTask<User> task = new LoadCollectionTask<User>();

        task.setContext(new ScActivity() {
            @Override
            public void onRefresh(boolean b) {
            }
        });





    }

}
