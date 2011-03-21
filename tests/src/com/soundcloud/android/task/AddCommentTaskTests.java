package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Comment;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;


@RunWith(RobolectricTestRunner.class)
public class AddCommentTaskTests {
    private SoundCloudApplication mApp;

    @Before
    public void setUp() {
        // XXX need to make this easier testable
        mApp = new SoundCloudApplication() {
            @Override
            protected String getConsumerKey(boolean production) {
                return "";
            }

            @Override
            protected String getConsumerSecret(boolean production) {
                return "";
            }
        };
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
