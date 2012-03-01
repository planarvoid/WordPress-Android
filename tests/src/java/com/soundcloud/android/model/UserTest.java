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
    public void testShouldIconLoad() throws Exception {
        User u = new User();
        expect(u.shouldLoadIcon()).toBeFalse();
        u.avatar_url = "";
        expect(u.shouldLoadIcon()).toBeFalse();
        u.avatar_url = "NULL";
        expect(u.shouldLoadIcon()).toBeFalse();
        u.avatar_url = "http://foo.com";
        expect(u.shouldLoadIcon()).toBeTrue();
    }

    @Test
    public void shouldGetPlan() throws Exception {
        User u = new User();
        expect(u.getPlan()).toBe(Plan.UNKNOWN);
        u.plan = "";
        expect(u.getPlan()).toBe(Plan.UNKNOWN);

        u.plan = "Pro plus";
        expect(u.getPlan()).toBe(Plan.PRO_PLUS);

        u.plan = "Pro";

        expect(u.getPlan()).toBe(Plan.PRO);
        u.plan = "Free";
        expect(u.getPlan()).toBe(Plan.FREE);

        u.plan = "lite";
        expect(u.getPlan()).toBe(Plan.LITE);
    }
}
