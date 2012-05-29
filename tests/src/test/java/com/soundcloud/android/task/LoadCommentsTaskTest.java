package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class LoadCommentsTaskTest {

    @Test
    public void shouldLoadComments() throws Exception {
        TestHelper.addCannedResponse(getClass(), "/tracks/100/comments?limit=50", "comments.json");
        LoadCommentsTask task = new LoadCommentsTask(DefaultTestRunner.application);

        List<Comment> comments = task.execute(100l).get();
        expect(comments).not.toBeNull();
        expect(comments.size()).toEqual(3);

        Comment comment = comments.get(0);
        expect(comment.user_id).toEqual(476254l);
        expect(comment.id).toEqual(24100348l);
        expect(comment.track_id).toEqual(21607568l);
        expect(comment.timestamp).toEqual(138751l);
        expect(comment.body).toEqual("wow, great voice!");
        expect(comment.created_at).toEqual(AndroidCloudAPI.CloudDateFormat.fromString("2011/08/22 11:35:24 +0000"));
        expect(comment.uri).toEqual("https://api.soundcloud.com/comments/24100348");

        expect(comment.user).not.toBeNull();
        expect(comment.user.id).toEqual(476254l);
    }

    @Test
    public void shouldSetTrackObjectOnCommentIfCached() throws Exception {
        Track t = new Track();
        t.id = 100;
        SoundCloudApplication.TRACK_CACHE.put(t);

        TestHelper.addCannedResponse(getClass(), "/tracks/100/comments?limit=50", "comments.json");
        LoadCommentsTask task = new LoadCommentsTask(DefaultTestRunner.application);

        List<Comment> comments = task.execute(t.id).get();
        expect(comments).not.toBeNull();

        for (Comment c : comments) {
            expect(c.track).toBe(t);
        }
        expect(t.comments).toBe(comments);
    }
}
