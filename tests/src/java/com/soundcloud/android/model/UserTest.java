package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;

@RunWith(DefaultTestRunner.class)
public class UserTest {

    @Test
    public void testLocation() throws Exception {
        User u = new User();
        expect(u.getLocation()).toEqual("");
        u.city = "Berlin";
        expect(u.getLocation()).toEqual("Berlin");
        u.country = "Germany";
        expect(u.getLocation()).toEqual("Berlin, Germany");
        u.city = null;
        expect(u.getLocation()).toEqual("Germany");
    }

    @Test
    public void testBuildContentValues() throws Exception {
        User u = new User();
        u.buildContentValues(false);
        u.buildContentValues(true);
        u.id = 1000L;
        ContentValues cv = u.buildContentValues(false);
        expect(cv.getAsLong(DBHelper.Users._ID)).toEqual(1000L);
    }

    @Test
    public void shouldPageTrack() throws Exception {
        User u = new User();
        u.permalink = "username";

        expect(u.pageTrack(false, "foo")).toEqual("/username/foo");
        expect(u.pageTrack(true, "foo")).toEqual("/you/foo");
    }
}
