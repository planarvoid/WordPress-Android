package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;

import java.util.Date;
import java.util.UUID;

@RunWith(DefaultTestRunner.class)
public class ActivityTest {
    @Test
    public void testEquals() throws Exception {
        Activity a1 = new Activity();
        Activity a2 = new Activity();

        a1.origin = new Track() { { id = 10L; } };
        a2.origin = new Track() { { id = 10L; } };

        expect(a1).toEqual(a2);

        a1.id = 20;  // Activity is not id based
        expect(a1).toEqual(a2);
        expect(a1.hashCode()).toEqual(a2.hashCode());

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
    public void shouldGenerateAGuidBasedOnCreatedAt() throws Exception {
        Activity a = new Activity();
        expect(a.toGUID()).toBeNull();
        a.created_at = AndroidCloudAPI.CloudDateFormat.fromString("2012/01/07 13:17:35 +0000");
        expect(a.toGUID()).toEqual("f6864180-3931-11e1-c000-000000000000");
    }

    @Test
    public void shouldGenerateAUUIDBasedOnCreatedAt() throws Exception {
        Activity a = new Activity();
        expect(a.toUUID()).toBeNull();
        a.created_at = AndroidCloudAPI.CloudDateFormat.fromString("2012/01/07 13:17:35 +0000");
        UUID uuid = a.toUUID();
        expect(uuid.version()).toEqual(1);
        expect(uuid.variant()).toEqual(6);
        expect(uuid.toString()).toEqual("f6864180-3931-11e1-c000-000000000000");
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

    @Test
    public void shouldGenerateADateString() throws Exception {
        Activity a = new Activity();
        final String date = "2012/01/07 13:17:35 +0000";
        a.created_at = AndroidCloudAPI.CloudDateFormat.fromString(date);
        expect(a.getDateString()).toEqual(date);
    }
}
