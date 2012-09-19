package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.os.Parcel;

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

    @Test
    public void shouldBeParcelable() throws Exception {
        User user = new User();
        user.id = 1;
        user.username = "peter";
        user.uri = "http://peter.com";
        user.avatar_url = "http://avatar.com";
        user.permalink = "peter";
        user.permalink_url = "http://peter.com";
        user.full_name = "Peter Test";
        user.description = "Peter Test";
        user.city = "Test City";
        user.country = "Test Country";
        user.plan = "solo";
        user.website = "http://blog.peter.com/";
        user.website_title = "Peters World";
        user.myspace_name = "peter93";
        user.discogs_name = "peter_discogs";
        user.track_count      = 1;
        user.followers_count  = 2;
        user.followings_count = 3;
        user.public_favorites_count = 4;
        user.private_tracks_count   = 5;


        Parcel p = Parcel.obtain();
        user.writeToParcel(p, 0);

        User u = new User(p);

        expect(u.id).toEqual(user.id);
        expect(u.username).toEqual(user.username);
        expect(u.uri).toEqual(user.uri);
        expect(u.avatar_url).toEqual(user.avatar_url);
        expect(u.permalink).toEqual(user.permalink);
        expect(u.permalink_url).toEqual(user.permalink_url);
        expect(u.full_name).toEqual(user.full_name);
        expect(u.description).toEqual(user.description);
        expect(u.city).toEqual(user.city);
        expect(u.country).toEqual(user.country);
        expect(u.plan).toEqual(user.plan);
        expect(u.website).toEqual(user.website);
        expect(u.website_title).toEqual(user.website_title);
        expect(u.myspace_name).toEqual(user.myspace_name);
        expect(u.discogs_name).toEqual(user.discogs_name);
        expect(u.track_count).toEqual(user.track_count);
        expect(u.followers_count).toEqual(user.followers_count);
        expect(u.followings_count).toEqual(user.followings_count);
        expect(u.public_favorites_count).toEqual(user.public_favorites_count);
        expect(u.private_tracks_count).toEqual(user.private_tracks_count);
    }
}
