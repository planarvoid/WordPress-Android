package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.os.Parcel;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class CommentTest {
    @Test
    public void shouldBeParcelable() throws Exception {
        Comment c1 = new Comment();
        c1.created_at = new Date();
        c1.user_id = 100L;
        c1.timestamp = 200L;
        c1.body = "Bodyz";
        c1.uri = "foo";

        User u = new User();
        u.username = "bert";

        c1.user = u;

        Parcel p = Parcel.obtain();
        c1.writeToParcel(p, 0);

        Comment c2 = Comment.CREATOR.createFromParcel(p);

        expect(c1.user_id).toEqual(c2.user_id);
        expect(c1.created_at).toEqual(c2.created_at);
        expect(c1.body).toEqual(c2.body);
        expect(c1.timestamp).toEqual(c2.timestamp);
        expect(c1.uri).toEqual(c2.uri);
        expect(c1.user.username).toEqual(c2.user.username);
    }

    @Test
    public void shouldBuildContentValues() throws Exception {
        Comment c1 = new Comment();
        c1.id = 100L;
        c1.created_at = new Date();
        c1.user_id = 100L;
        c1.timestamp = 200L;
        c1.body = "Bodyz";
        c1.uri = "foo";

        ContentValues cv = c1.buildContentValues();

        expect(cv).not.toBeNull();
        expect(cv.getAsLong("_id")).toEqual(c1.id);
        expect(cv.getAsLong("created_at")).toEqual(c1.created_at.getTime());
        expect(cv.getAsLong("user_id")).toEqual(c1.user_id);
        expect(cv.getAsLong("track_id")).toEqual(c1.track_id);
        expect(cv.getAsLong("timestamp")).toEqual(c1.timestamp);
        expect(cv.getAsString("body")).toEqual(c1.body);
    }

    @Test
    public void shouldNotCrashWithDivideByZero() throws Exception {
        new Comment().calculateXPos(40, 0);
    }
}
