package com.soundcloud.task;


import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.task.LoadConnectionsTask;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
public class LoadConnectionTaskTests {

    @Before public void setUp() throws Exception {
        Robolectric.getBackgroundScheduler().pause();
        Robolectric.getUiThreadScheduler().pause();
    }

    @Test public void test() throws ExecutionException, InterruptedException {
        SoundCloudApplication app = new SoundCloudApplication();
        LoadConnectionsTask task = new LoadConnectionsTask(app);
        task.execute();

        Robolectric.runBackgroundTasks();
    }
}
