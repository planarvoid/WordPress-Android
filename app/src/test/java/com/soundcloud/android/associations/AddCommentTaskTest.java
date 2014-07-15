package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Comment;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

import java.io.IOException;


@RunWith(DefaultTestRunner.class)
public class AddCommentTaskTest {

    private AddCommentTask task;

    @Before
    public void setUp(){
        task = new AddCommentTask(DefaultTestRunner.application, DefaultTestRunner.application.getCloudAPI());
    }
    @Test
    public void shouldPostComment() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        mockSuccessfulCommentCreation();

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
        SoundCloudApplication.sModelManager.cache(new PublicApiTrack(100l));

        mockSuccessfulCommentCreation();
        expect(task.execute(c).get()).not.toBeNull();

        expect(findBroadcast(Playable.COMMENTS_UPDATED)).not.toBeNull();
    }

    @Test
    public void shouldNotPostCommentWhenHttpError() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        Robolectric.addHttpResponseRule("/tracks/100/comments", new TestHttpResponse(400, "FAILZ"));
        expect(task.execute(c).get()).toBeNull();
    }

    @Test
    public void shouldNotPostCommentWhenException() throws Exception {
        Comment c = new Comment();
        c.track_id = 100;
        SoundCloudApplication.sModelManager.cache(new PublicApiTrack(100l));
        TestHelper.addPendingIOException("/tracks/100/comments");
        expect(task.execute(c).get()).toBeNull();
        expect(findBroadcast(Actions.CONNECTION_ERROR)).not.toBeNull();
        expect(findBroadcast(Playable.COMMENTS_UPDATED)).not.toBeNull();
    }

    @Test
    public void shouldUseIdFromTrackIfAvailable() throws Exception {
        Comment c = new Comment();
        c.track = new PublicApiTrack();
        c.track.setId(100);
        mockSuccessfulCommentCreation();
        expect(task.execute(c).get()).not.toBeNull();
    }

    @Test
    public void shouldFailWithoutTrackId() throws Exception {
        Comment c = new Comment();
        expect(task.execute(c).get()).toBeNull();
    }

    @Test
    public void shouldAddCommentToCache() throws Exception {
        Comment c = new Comment();
        c.track = new PublicApiTrack();
        c.track.setId(100);
        SoundCloudApplication.sModelManager.cache(c.track);

        mockSuccessfulCommentCreation();
        expect(task.execute(c).get()).not.toBeNull();
        expect(c.track.comments.size()).toBe(1);
    }

    private Intent findBroadcast(final String action) {
        return Iterables.find(DefaultTestRunner.application.broadcasts, new Predicate<Intent>() {
            @Override
            public boolean apply(Intent input) {
                return input.getAction().equals(action);
            }
        });
    }
}
