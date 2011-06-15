package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;


@RunWith(DefaultTestRunner.class)
public class AddCommentTaskTest {
    private SoundCloudApplication mApp;

    @Before
    public void setUp() {
        mApp = (SoundCloudApplication) Robolectric.application;
        mApp.onCreate();
    }

    @Test
    public void shouldPostComment() throws Exception {
        Comment c = new Comment();

        Robolectric.addPendingHttpResponse(201, "OK");
        AddCommentTask task = new AddCommentTask(mApp, null);
        assertThat(task.doInBackground(c), equalTo(true));
    }

    @Test
    public void shouldNotPostCommentWhenError() throws Exception {
        Comment c = new Comment();

        Robolectric.addPendingHttpResponse(400, "FAILZ");
        AddCommentTask task = new AddCommentTask(mApp, null);
        assertThat(task.doInBackground(c), equalTo(false));
    }
}
