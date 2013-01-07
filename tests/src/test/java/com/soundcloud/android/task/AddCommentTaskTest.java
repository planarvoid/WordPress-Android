package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.Actions;
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

import java.io.IOException;


@RunWith(DefaultTestRunner.class)
public class AddCommentTaskTest {
    @Test
    public void shouldPostComment() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        mockSuccessfulCommentCreation();

        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).not.toBeNull();
    }

    private void mockSuccessfulCommentCreation() throws IOException {
        Robolectric.getFakeHttpLayer().addHttpResponseRule("/tracks/100/comments",
                new TestHttpResponse(201, TestHelper.resourceAsBytes(getClass(), "comment.json")));
    }

    @Test
    public void shouldSendBroadcastAfterPost() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        SoundCloudApplication.MODEL_MANAGER.cache(new Track(100l));

        mockSuccessfulCommentCreation();
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).not.toBeNull();

        expect(DefaultTestRunner.application.broadcasts.size()).toEqual(1);
        expect(DefaultTestRunner.application.broadcasts.get(0).getAction()).toEqual(Sound.COMMENTS_UPDATED);
    }

    @Test
    public void shouldNotPostCommentWhenHttpError() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        Robolectric.addHttpResponseRule("/tracks/100/comments", new TestHttpResponse(400, "FAILZ"));
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).toBeNull();
    }

    @Test
    public void shouldNotPostCommentWhenException() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        SoundCloudApplication.MODEL_MANAGER.cache(new Track(100l));
        TestHelper.addPendingIOException("/tracks/100/comments");
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).toBeNull();
        expect(DefaultTestRunner.application.broadcasts.get(0).getAction()).toEqual(Actions.CONNECTION_ERROR);
        expect(DefaultTestRunner.application.broadcasts.get(1).getAction()).toEqual(Sound.COMMENTS_UPDATED);
    }

    @Test
    public void shouldUseIdFromTrackIfAvailable() throws Exception {
        Comment c = new Comment();
        c.track = new Track();
        c.track.id = 100;
        mockSuccessfulCommentCreation();
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
        SoundCloudApplication.MODEL_MANAGER.cache(c.track);

        mockSuccessfulCommentCreation();
        AddCommentTask task = new AddCommentTask(DefaultTestRunner.application);
        expect(task.execute(c).get()).not.toBeNull();
        expect(c.track.comments.size()).toBe(1);
    }
}
