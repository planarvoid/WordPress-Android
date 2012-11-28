package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.model.act.CommentActivity;
import com.soundcloud.android.model.act.TrackActivity;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;

import java.util.Date;
import java.util.UUID;

@RunWith(DefaultTestRunner.class)
public class ActivityTest {

    @Test
    public void shouldGenerateAGuidBasedOnCreatedAt() throws Exception {
        Activity a = new TrackActivity();
        expect(a.toGUID()).toBeNull();
        a.created_at = AndroidCloudAPI.CloudDateFormat.fromString("2012/01/07 13:17:35 +0000");
        expect(a.toGUID()).toEqual("f6864180-3931-11e1-c000-000000000000");
    }

    @Test
    public void shouldGenerateAUUIDBasedOnCreatedAt() throws Exception {
        Activity a = new TrackActivity();
        expect(a.toUUID()).toBeNull();
        a.created_at = AndroidCloudAPI.CloudDateFormat.fromString("2012/01/07 13:17:35 +0000");
        UUID uuid = a.toUUID();
        expect(uuid.version()).toEqual(1);
        expect(uuid.variant()).toEqual(6);
        expect(uuid.toString()).toEqual("f6864180-3931-11e1-c000-000000000000");
    }

    @Test
    public void shouldBuildContentValues() throws Exception {
        TrackActivity a = new TrackActivity();
        final Date date = new Date();
        a.created_at = date;
        a.tags = "foo";
        a.track = new Track() { { id = 10L; } };

        ContentValues cv = a.buildContentValues();

        expect(cv.getAsString(DBHelper.Activities.TAGS)).toEqual("foo");
        expect(cv.getAsString(DBHelper.Activities.TYPE)).toEqual(Activity.Type.TRACK.type);
        expect(cv.getAsLong(DBHelper.Activities.SOUND_ID)).toEqual(10L);
        expect(cv.getAsLong(DBHelper.Activities.CREATED_AT)).toEqual(date.getTime());
    }

    @Test
    public void shouldBuildContentValuesForCommentActivity() throws Exception {
        CommentActivity a = new CommentActivity();
        final Date date = new Date();
        a.created_at = date;
        a.tags = "foo";
        a.comment = new Comment() { { id = 10L; } };

        ContentValues cv = a.buildContentValues();
        expect(cv.getAsString(DBHelper.Activities.TAGS)).toEqual("foo");
        expect(cv.getAsString(DBHelper.Activities.TYPE)).toEqual(Activity.Type.COMMENT.type);
        expect(cv.getAsLong(DBHelper.Activities.COMMENT_ID)).toEqual(10L);
        expect(cv.getAsLong(DBHelper.Activities.CREATED_AT)).toEqual(date.getTime());
    }

    @Test
    public void shouldGenerateADateString() throws Exception {
        TrackActivity a = new TrackActivity();
        final String date = "2012/01/07 13:17:35 +0000";
        a.created_at = AndroidCloudAPI.CloudDateFormat.fromString(date);
        expect(a.getDateString()).toEqual(date);
    }
}
