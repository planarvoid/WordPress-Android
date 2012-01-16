package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class ActivityTest {
    @Test
    public void testEquals() throws Exception {
        Activity a1 = new Activity();
        Activity a2 = new Activity();

        a1.origin = new Track() { { id = 10L; } };
        a2.origin = new Track() { { id = 10L; } };

        expect(a1).toEqual(a2);

        Date d = new Date();
        a1.created_at = d;
        expect(a1).not.toEqual(a2);
        a2.created_at = d;

        expect(a1).toEqual(a2);

        a1.tags = "foo";

        expect(a1).not.toEqual(a2);
        a2.tags = a1.tags;

        expect(a1).toEqual(a2);

        a1.type = Activity.Type.TRACK;
        expect(a1).not.toEqual(a2);
        a2.type = a1.type;
        expect(a1).toEqual(a2);
    }

    @Test
    public void shouldBuildContentValues() throws Exception {
        Activity a = new Activity();
        final Date date = new Date();
        a.created_at = date;
        a.tags = "foo";
        a.type = Activity.Type.TRACK;
        a.origin = new Track() { { id = 10L; } };

        ContentValues cv = a.buildContentValues();

        expect(cv.getAsString("tags")).toEqual("foo");
        expect(cv.getAsString("type")).toEqual("track");
        expect(cv.getAsLong("track_id")).toEqual(10L);
        expect(cv.getAsLong("created_at")).toEqual(date.getTime());
    }

    @Test
    public void shouldBuildContentValuesForCommentActivity() throws Exception {
        Activity a = new Activity();
        final Date date = new Date();
        a.created_at = date;
        a.tags = "foo";
        a.type = Activity.Type.COMMENT;
        a.origin = new Comment() { { id = 10L; } };

        ContentValues cv = a.buildContentValues();
        expect(cv.getAsString("tags")).toEqual("foo");
        expect(cv.getAsString("type")).toEqual("comment");
        expect(cv.getAsLong("comment_id")).toEqual(10L);
        expect(cv.getAsLong("created_at")).toEqual(date.getTime());
    }

}
