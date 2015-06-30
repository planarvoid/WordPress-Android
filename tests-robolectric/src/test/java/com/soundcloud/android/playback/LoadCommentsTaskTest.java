package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class LoadCommentsTaskTest {

    @Test
    public void shouldLoadComments() throws Exception {
        TestHelper.addCannedResponse(getClass(), "/tracks/100/comments?limit=50", "comments.json");
        LoadCommentsTask task = new LoadCommentsTask(DefaultTestRunner.application.getCloudAPI());

        List<PublicApiComment> comments = task.execute(100l).get();
        expect(comments).not.toBeNull();
        expect(comments.size()).toEqual(3);

        PublicApiComment comment = comments.get(0);
        expect(comment.user_id).toEqual(476254l);
        expect(comment.getId()).toEqual(24100348l);
        expect(comment.track_id).toEqual(21607568l);
        expect(comment.timestamp).toEqual(138751l);
        expect(comment.body).toEqual("wow, great voice!");
        expect(comment.getCreatedAt()).toEqual(PublicApi.CloudDateFormat.fromString("2011/08/22 11:35:24 +0000"));
        expect(comment.uri).toEqual("https://api.soundcloud.com/comments/24100348");

        expect(comment.user).not.toBeNull();
        expect(comment.user.getId()).toEqual(476254l);
    }

    @Test
    public void shouldSetTrackObjectOnCommentIfCached() throws Exception {
        PublicApiTrack t = new PublicApiTrack();
        t.setId(100);
        SoundCloudApplication.sModelManager.cache(t);

        TestHelper.addCannedResponse(getClass(), "/tracks/100/comments?limit=50", "comments.json");
        LoadCommentsTask task = new LoadCommentsTask(DefaultTestRunner.application.getCloudAPI());

        List<PublicApiComment> comments = task.execute(t.getId()).get();
        expect(comments).not.toBeNull();

        for (PublicApiComment c : comments) {
            expect(c.track).toBe(t);
        }
        expect(t.comments).toBe(comments);
    }
}
