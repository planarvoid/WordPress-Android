package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
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
        u.setCity("Berlin");
        expect(u.getLocation()).toEqual("Berlin");
        u.setCountry("Germany");
        expect(u.getLocation()).toEqual("Berlin, Germany");
        u.setCity(null);
        expect(u.getLocation()).toEqual("Germany");
    }

    @Test
    public void testBuildContentValues() throws Exception {
        User u = new User();
        u.mID = 1000L;
        ContentValues cv = u.buildContentValues();
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
    public void shouldGetWebsiteTitle() throws Exception {
        User u = new User();
        expect(u.getWebSiteTitle()).toBeNull();
        u.website = "http://foo.com";
        expect(u.getWebSiteTitle()).toEqual("foo.com");
        u.website_title = "Foo";
        expect(u.getWebSiteTitle()).toEqual("Foo");
    }

    @Test
    public void shouldBeParcelable() throws Exception {
        User user = new User();
        user.mID = 1;
        user.username = "peter";
        user.uri = "http://peter.com";
        user.avatar_url = "http://avatar.com";
        user.permalink = "peter";
        user.permalink_url = "http://peter.com";
        user.full_name = "Peter Test";
        user.description = "Peter Test";
        user.setCity("Test City");
        user.setCountry("Test Country");
        user.plan = "solo";
        user.website = "http://blog.peter.com/";
        user.website_title = "Peters World";
        user.myspace_name = "peter93";
        user.discogs_name = "peter_discogs";
        user.track_count = 1;
        user.followers_count = 2;
        user.followings_count = 3;
        user.public_likes_count = 4;
        user.private_tracks_count = 5;


        Parcel p = Parcel.obtain();
        user.writeToParcel(p, 0);

        User u = new User(p);

        expect(u.mID).toEqual(user.mID);
        expect(u.username).toEqual(user.username);
        expect(u.uri).toEqual(user.uri);
        expect(u.avatar_url).toEqual(user.avatar_url);
        expect(u.permalink).toEqual(user.permalink);
        expect(u.permalink_url).toEqual(user.permalink_url);
        expect(u.full_name).toEqual(user.full_name);
        expect(u.description).toEqual(user.description);
        expect(u.getCity()).toEqual(user.getCity());
        expect(u.getCountry()).toEqual(user.getCountry());
        expect(u.plan).toEqual(user.plan);
        expect(u.website).toEqual(user.website);
        expect(u.website_title).toEqual(user.website_title);
        expect(u.myspace_name).toEqual(user.myspace_name);
        expect(u.discogs_name).toEqual(user.discogs_name);
        expect(u.track_count).toEqual(user.track_count);
        expect(u.followers_count).toEqual(user.followers_count);
        expect(u.followings_count).toEqual(user.followings_count);
        expect(u.public_likes_count).toEqual(user.public_likes_count);
        expect(u.private_tracks_count).toEqual(user.private_tracks_count);
    }

    @Test
    public void shouldDeserializeUser() throws Exception {
        User u = TestHelper.readJson(User.class, "/com/soundcloud/android/model/user.json");

        expect(u.mID).not.toBeNull();
        expect(u.username).not.toBeNull();
        expect(u.uri).not.toBeNull();
        expect(u.avatar_url).not.toBeNull();
        expect(u.permalink).not.toBeNull();
        expect(u.permalink_url).not.toBeNull();
        expect(u.full_name).not.toBeNull();
        expect(u.description).not.toBeNull();
        expect(u.getCity()).not.toBeNull();
        expect(u.getCountry()).not.toBeNull();
        expect(u.website).not.toBeNull();
        expect(u.website_title).not.toBeNull();
        expect(u.track_count).not.toBeNull();
        expect(u.followers_count).not.toBeNull();
        expect(u.followings_count).not.toBeNull();
        expect(u.public_likes_count).not.toBeNull();
        expect(u.private_tracks_count).not.toBeNull();
    }

    @Test
    public void shouldNotIncreaseFollowerCountIfNotSet() throws Exception {
        User u = new User();
        u.followers_count = User.NOT_SET;
        expect(u.addAFollower()).toBeFalse();
        expect(u.followers_count).toEqual(User.NOT_SET);
    }

    @Test
    public void shouldIncreaseFollowerCountIfSet() throws Exception {
        User u = new User();
        u.followers_count = 1;
        expect(u.addAFollower()).toBeTrue();
        expect(u.followers_count).toEqual(2);
    }

    @Test
    public void shouldNotDecreaseFollowerCountIfNotSet() throws Exception {
        User u = new User();
        u.followers_count = User.NOT_SET;
        expect(u.removeAFollower()).toBeFalse();
        expect(u.followers_count).toEqual(User.NOT_SET);
    }

    @Test
    public void shouldDecreaseFollowerCountIfSet() throws Exception {
        User u = new User();
        u.followers_count = 1;
        expect(u.removeAFollower()).toBeTrue();
        expect(u.followers_count).toEqual(0);
    }
}
