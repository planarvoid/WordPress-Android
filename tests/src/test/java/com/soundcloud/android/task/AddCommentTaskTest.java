package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Sound;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class AddCommentTaskTest {
    @Test
    public void shouldPostComment() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        Robolectric.getFakeHttpLayer().addHttpResponseRule("/tracks/100/comments",
                        new TestHttpResponse(201, TestHelper.resourceAsBytes(getClass(), "comment.json")));

        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application, c);
        expect(task.execute().get()).not.toBeNull();
    }

    @Test
    public void shouldSendBroadcastAfterPost() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        SoundCloudApplication.MODEL_MANAGER.cache(new Track(100l));
        Robolectric.getFakeHttpLayer().addHttpResponseRule("/tracks/100/comments",
                                new TestHttpResponse(201, TestHelper.resourceAsBytes(getClass(), "comment.json")));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application, c);
        expect(task.execute().get()).not.toBeNull();

        expect(DefaultTestRunner.application.broadcasts.size()).toEqual(1);
        expect(DefaultTestRunner.application.broadcasts.get(0).getAction()).toEqual(Sound.COMMENTS_UPDATED);
    }

    @Test
    public void shouldNotPostCommentWhenError() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        Robolectric.addHttpResponseRule("/tracks/100/comments", new TestHttpResponse(400, "FAILZ"));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application, c);
        expect(task.execute().get()).toBeNull();
    }

    @Test
    public void shouldUseIdFromTrackIfAvailable() throws Exception {
        Comment c = new Comment();
        c.track = new Track();
        c.track.id = 100;
        Robolectric.getFakeHttpLayer().addHttpResponseRule("/tracks/100/comments",
                new TestHttpResponse(201, TestHelper.resourceAsBytes(getClass(), "comment.json")));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application, c);
        expect(task.execute().get()).not.toBeNull();
    }

    @Test
    public void shouldFailWithoutTrackId() throws Exception {
        Comment c = new Comment();
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application, c);
        expect(task.execute().get()).toBeNull();
    }

    @Test
    public void shouldAddCommentToCache() throws Exception {
        Comment c = new Comment();
        c.track = new Track();
        c.track.id = 100;
        SoundCloudApplication.MODEL_MANAGER.cache(c.track);

        Robolectric.getFakeHttpLayer().addHttpResponseRule("/tracks/100/comments",
                new TestHttpResponse(201, TestHelper.resourceAsBytes(getClass(), "comment.json")));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application, c);
        expect(task.execute().get()).not.toBeNull();
        expect(c.track.comments.size()).toBe(1);
    }
}
