package com.soundcloud.android.api.legacy.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.content.ContentValues;
import android.os.Parcel;

import java.util.Date;

public class PublicApiCommentTest extends AndroidUnitTest {

    @Test
    public void shouldBeParcelable() throws Exception {
        PublicApiComment c1 = new PublicApiComment();
        c1.setCreatedAt(new Date());
        c1.user_id = 100L;
        c1.timestamp = 200L;
        c1.body = "Bodyz";
        c1.uri = "foo";

        PublicApiUser u = new PublicApiUser();
        u.username = "bert";

        c1.user = u;

        Parcel p = Parcel.obtain();
        c1.writeToParcel(p, 0);
        p.setDataPosition(0);

        PublicApiComment c2 = PublicApiComment.CREATOR.createFromParcel(p);

        assertThat(c1.user_id).isEqualTo(c2.user_id);
        assertThat(c1.getCreatedAt()).isEqualTo(c2.getCreatedAt());
        assertThat(c1.body).isEqualTo(c2.body);
        assertThat(c1.timestamp).isEqualTo(c2.timestamp);
        assertThat(c1.uri).isEqualTo(c2.uri);
        assertThat(c1.user.username).isEqualTo(c2.user.username);
    }

    @Test
    public void shouldBuildContentValues() throws Exception {
        PublicApiComment c1 = new PublicApiComment();
        c1.setId(100L);
        c1.setCreatedAt(new Date());
        c1.user_id = 100L;
        c1.timestamp = 200L;
        c1.body = "Bodyz";
        c1.uri = "foo";

        ContentValues cv = c1.buildContentValues();

        assertThat(cv).isNotNull();
        assertThat(cv.getAsLong("_id")).isEqualTo(c1.getId());
        assertThat(cv.getAsLong("created_at")).isEqualTo(c1.getCreatedAt().getTime());
        assertThat(cv.getAsLong("user_id")).isEqualTo(c1.user_id);
        assertThat(cv.getAsLong("track_id")).isEqualTo(c1.track_id);
        assertThat(cv.getAsLong("timestamp")).isEqualTo(c1.timestamp);
        assertThat(cv.getAsString("body")).isEqualTo(c1.body);
    }

    @Test
    public void shouldNotCrashWithDivideByZero() throws Exception {
        new PublicApiComment().calculateXPos(40, 0);
    }
}
