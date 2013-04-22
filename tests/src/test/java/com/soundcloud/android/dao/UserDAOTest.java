package com.soundcloud.android.dao;

import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

public class UserDAOTest extends AbstractDAOTest<UserDAO> {
    public UserDAOTest() {
        super(new UserDAO(Robolectric.application.getContentResolver()));
    }

    @Test
    public void test() throws Exception {
        // TODO: test previously untested methods!
    }


    // scmodel manager tests


    /*
    @Test
    public void shouldGetUserById() throws Exception {
        User u = new User();
        u.id = 100L;
        u.permalink = "foo";

        Uri uri = u.insert(resolver);

        expect(uri).not.toBeNull();

        User u2 = manager.getUser(100);
        expect(u2).not.toBeNull();
        expect(u2.id).toEqual(u.id);
        expect(u2.permalink).toEqual(u.permalink);
    }



    @Test
    public void shouldNotGetUserByIdNegative() throws Exception {
        expect(manager.getUser(-1l)).toBeNull();
    }





    @Test
    public void shouldInsertUserAndReadBackUser() throws Exception {
        User u = new User();
        u.id = 100L;
        u.full_name = "Bobby Fuller";
        u.permalink = "foo";
        u.description = "baz";
        u.city = "Somewhere";
        u.plan = "plan";
        u.website = "http://foo.com";
        u.website_title = "Site";
        u.setPrimaryEmailConfirmed(true);
        u.myspace_name = "myspace";
        u.discogs_name = "discogs";

        int counter = 0;
        u.track_count = ++counter;
        u.followers_count = ++counter;
        u.followings_count = ++counter;
        u.public_likes_count = ++counter;
        u.private_tracks_count = ++counter;

        Uri uri = manager.write(u);

        expect(uri).not.toBeNull();

        User u2 = manager.getUser(uri);
        expect(u2).not.toBeNull();
        expect(u2.full_name).toEqual(u.full_name);
        expect(u2.permalink).toEqual(u.permalink);
        expect(u2.city).toEqual(u.city);
        expect(u2.plan).toEqual(u.plan);
        expect(u2.website).toEqual(u.website);
        expect(u2.website_title).toEqual(u.website_title);
        expect(u2.isPrimaryEmailConfirmed()).toEqual(u.isPrimaryEmailConfirmed());
        expect(u2.myspace_name).toEqual(u.myspace_name);
        expect(u2.discogs_name).toEqual(u.discogs_name);

        expect(u2.track_count).toEqual(u.track_count);
        expect(u2.followers_count).toEqual(u.followers_count);
        expect(u2.followings_count).toEqual(u.followings_count);
        expect(u2.public_likes_count).toEqual(u.public_likes_count);
        expect(u2.private_tracks_count).toEqual(u.private_tracks_count);

        expect(u2.last_updated).not.toEqual(u.last_updated);

        // description is not store
        expect(u2.description).toBeNull();
    }


 @Test
    public void shouldUpsertUser() throws Exception {
        User u = new User();
        u.id = 100L;
        u.permalink = "foo";
        u.description = "baz";

        Uri uri = manager.write(u);

        expect(uri).not.toBeNull();

        User u2 = manager.getUser(uri);

        u2.permalink = "nomnom";

        manager.write(u2);

        User u3 = manager.getUser(uri);

        expect(u3).not.toBeNull();
        expect(u3.permalink).toEqual("nomnom");
        expect(u3.id).toEqual(100L);
    }

    */



}
