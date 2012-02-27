package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
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
        Robolectric.addHttpResponseRule("/tracks/100/comments", new TestHttpResponse(201, "OK"));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).not.toBeNull();
    }

    @Test
    public void shouldSendBroadcastAfterPost() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        Robolectric.addHttpResponseRule("/tracks/100/comments", new TestHttpResponse(201, "OK"));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).not.toBeNull();

        expect(DefaultTestRunner.application.broadcasts.size()).toEqual(1);
        expect(DefaultTestRunner.application.broadcasts.get(0).getAction()).toEqual(Actions.COMMENT_ADDED);
    }

    @Test
    public void shouldNotPostCommentWhenError() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        Robolectric.addHttpResponseRule("/tracks/100/comments", new TestHttpResponse(400, "FAILZ"));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).toBeNull();
    }

    @Test
    public void shouldUseIdFromTrackIfAvailable() throws Exception {
        Comment c = new Comment();
        c.track = new Track();
        c.track.id = 100;
        Robolectric.addHttpResponseRule("/tracks/100/comments", new TestHttpResponse(201, "OK"));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).not.toBeNull();
    }

    @Test
    public void shouldFailWithoutTrackId() throws Exception {
        Comment c = new Comment();
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).toBeNull();
    }

    @Test
    public void shouldAddCommentToCache() throws Exception {
        Comment c = new Comment();
        c.track = new Track();
        c.track.id = 100;
        SoundCloudApplication.TRACK_CACHE.put(c.track);

        Robolectric.addHttpResponseRule("/tracks/100/comments", new TestHttpResponse(201, "OK"));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).not.toBeNull();
        expect(c.track.comments).toContainExactly(c);
    }
}
