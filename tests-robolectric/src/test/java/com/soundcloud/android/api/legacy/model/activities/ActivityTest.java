package com.soundcloud.android.api.legacy.model.activities;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.SharingNote;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.TableColumns;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.database.MatrixCursor;

import java.util.Date;
import java.util.UUID;

@RunWith(DefaultTestRunner.class)
public class ActivityTest {

    @Test
    public void shouldUseUuidAndIgnoreCreatedAt() throws Exception {
        Activity a = new TrackActivity();
        expect(a.toGUID()).toBeNull();

        final String uuidStr = "ffffffff-1111-11e1-c000-000000000000";
        a.uuid = uuidStr;
        a.setCreatedAt(PublicApi.CloudDateFormat.fromString("2012/01/07 13:17:35 +0000"));

        expect(a.toUUID().toString()).toEqual(uuidStr);
        expect(a.toGUID()).toEqual(uuidStr);
    }

    @Test
    public void shouldGenerateAGuidBasedOnCreatedAt() throws Exception {
        Activity a = new TrackActivity();
        expect(a.toGUID()).toBeNull();
        a.setCreatedAt(PublicApi.CloudDateFormat.fromString("2012/01/07 13:17:35 +0000"));
        expect(a.toGUID()).toEqual("f6864180-3931-11e1-c000-000000000000");
    }

    @Test
    public void shouldGenerateAUUIDBasedOnCreatedAt() throws Exception {
        Activity a = new TrackActivity();
        expect(a.toUUID()).toBeNull();
        a.setCreatedAt(PublicApi.CloudDateFormat.fromString("2012/01/07 13:17:35 +0000"));
        UUID uuid = a.toUUID();
        expect(uuid.version()).toEqual(1);
        expect(uuid.variant()).toEqual(6);
        expect(uuid.toString()).toEqual("f6864180-3931-11e1-c000-000000000000");
    }

    @Test
    public void shouldBuildContentValues() throws Exception {
        TrackActivity a = new TrackActivity();
        final Date date = new Date();
        a.setCreatedAt(date);
        a.tags = "foo";
        a.track = new PublicApiTrack() { { setId(10L); } };

        ContentValues cv = a.buildContentValues();

        expect(cv.getAsString(TableColumns.Activities.TAGS)).toEqual("foo");
        expect(cv.getAsString(TableColumns.Activities.TYPE)).toEqual(Activity.Type.TRACK.type);
        expect(cv.getAsLong(TableColumns.Activities.SOUND_ID)).toEqual(10L);
        expect(cv.getAsLong(TableColumns.Activities.CREATED_AT)).toEqual(date.getTime());
    }

    @Test
    public void shouldBuildContentValuesForCommentActivity() throws Exception {
        CommentActivity a = new CommentActivity();
        final Date date = new Date();
        a.setCreatedAt(date);
        a.tags = "foo";
        a.comment = new PublicApiComment() { { setId(10L); } };

        ContentValues cv = a.buildContentValues();
        expect(cv.getAsString(TableColumns.Activities.TAGS)).toEqual("foo");
        expect(cv.getAsString(TableColumns.Activities.TYPE)).toEqual(Activity.Type.COMMENT.type);
        expect(cv.getAsLong(TableColumns.Activities.COMMENT_ID)).toEqual(10L);
        expect(cv.getAsLong(TableColumns.Activities.CREATED_AT)).toEqual(date.getTime());
    }

    @Test
    public void shouldBuildContentValuesForUserMentionActivity() throws Exception {
        UserMentionActivity a = new UserMentionActivity();
        final Date date = new Date();
        a.setCreatedAt(date);
        a.tags = "foo";
        a.comment = new PublicApiComment() { { setId(10L); } };

        ContentValues cv = a.buildContentValues();
        expect(cv.getAsString(TableColumns.Activities.TAGS)).toEqual("foo");
        expect(cv.getAsString(TableColumns.Activities.TYPE)).toEqual(Activity.Type.USER_MENTION.type);
        expect(cv.getAsLong(TableColumns.Activities.COMMENT_ID)).toEqual(10L);
        expect(cv.getAsLong(TableColumns.Activities.CREATED_AT)).toEqual(date.getTime());
    }

    @Test
    public void shouldGenerateADateString() throws Exception {
        TrackActivity a = new TrackActivity();
        final String date = "2012/01/07 13:17:35 +0000";
        a.setCreatedAt(PublicApi.CloudDateFormat.fromString(date));
        expect(a.getDateString()).toEqual(date);
    }

    @Test
    public void allActivityTypesShouldBeInstantiableFromCursor() throws Exception {
        MatrixCursor cursor = new MatrixCursor(TableColumns.ActivityView.ALL_FIELDS);
        cursor.addRow(new Object[TableColumns.ActivityView.ALL_FIELDS.length]);
        cursor.moveToFirst();

        for (Activity.Type t : Activity.Type.values()) {
            Activity instance = t.fromCursor(cursor);
            expect(instance).not.toBeNull();
            expect(instance.getClass().isAssignableFrom(t.activityClass)).toBeTrue();
        }
    }

    @Test
    public void shouldNotBeEqualWithDifferentUUID() throws Exception {
        TrackActivity a1 = new TrackActivity();
        TrackActivity a2 = new TrackActivity();
        a1.uuid = "12345";
        a2.uuid = "54321";
        a1.tags = a2.tags = "abc def";
        Date date = PublicApi.CloudDateFormat.fromString("2012/01/07 13:17:35 +0000");
        a1.setCreatedAt(date);
        a2.setCreatedAt(date);
        a1.sharing_note = new SharingNote();
        expect(a1).not.toEqual(a2);
    }

    @Test
    public void shouldBeEqualWithDifferentSharingNotes() throws Exception {
        TrackActivity a1 = new TrackActivity();
        TrackActivity a2 = new TrackActivity();
        a1.uuid = a2.uuid = "12345";
        a1.tags = a2.tags = "abc def";
        Date date = PublicApi.CloudDateFormat.fromString("2012/01/07 13:17:35 +0000");
        a1.setCreatedAt(date);
        a2.setCreatedAt(date);
        a1.sharing_note = new SharingNote();
        expect(a1).toEqual(a2);
    }
}
