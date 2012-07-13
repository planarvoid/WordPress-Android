package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import com.xtremelabs.robolectric.shadows.ShadowAdapterView;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;


@RunWith(DefaultTestRunner.class)
public class UserBrowserTest {

    public static final long USER_ID = 3135930l;
    UserBrowser browser;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        browser = new UserBrowser() {};
        // TODO should fix race condition in LazyEndlessAdapter
        ShadowAdapterView.automaticallyUpdateRowViews(false);
    }

    @AfterClass public static void after() { ShadowAdapterView.automaticallyUpdateRowViews(true); }

    @Test @DisableStrictI18n
    public void testOnCreateYou() throws Exception {
        Robolectric.getFakeHttpLayer().setDefaultHttpResponse(404, "");

        browser.setIntent(new Intent());
        browser.onCreate(null);
        expect(browser.mUser.id).toEqual(3135930l);
        expect(browser.mUser.username).toBeNull();

        // test rest of lifecycle for crashes
        browser.onStart();
        browser.onResume();
    }

    @Test @DisableStrictI18n
    public void testOnCreateYouDb() throws Exception {
        Robolectric.getFakeHttpLayer().setDefaultHttpResponse(404, "");
        SoundCloudDB.insertUser(Robolectric.application.getContentResolver(),getUserWithUsername());

        browser.setIntent(new Intent());
        browser.onCreate(null);
        expect(browser.mUser.id).toEqual(3135930l);
        expect(browser.mUser.username).toEqual("SoundCloud Android @ MWC");
    }

    @Test @DisableStrictI18n
    public void testOnCreateFetchYou() throws Exception {
        Robolectric.addHttpResponseRule("/users/"+USER_ID, TestHelper.resource(getClass(), "user.json"));
        Robolectric.getFakeHttpLayer().setDefaultHttpResponse(404, "");

        browser.setIntent(new Intent());
        browser.onCreate(null);
        expect(browser.mUser.id).toEqual(3135930l);
        expect(browser.mUser.username).toEqual("SoundCloud Android @ MWC");
        expect(browser.mUser.country).toEqual("Germany");
    }

    @Test @DisableStrictI18n
    public void testOnCreateOtherId() throws Exception {
        Robolectric.getFakeHttpLayer().setDefaultHttpResponse(404, "");

        browser.setIntent(new Intent().putExtra("userId",3135930l));
        browser.onCreate(null);
        expect(browser.mUser.id).toEqual(3135930l);
        expect(browser.mUser.username).toBeNull();

        // test rest of lifecycle for crashes
        browser.onStart();
        browser.onResume();
    }

    @Test @DisableStrictI18n
    public void testOnCreateOtherIdDb() throws Exception {
        Robolectric.getFakeHttpLayer().setDefaultHttpResponse(404, "");
        SoundCloudDB.insertUser(Robolectric.application.getContentResolver(),getUserWithUsername());

        browser.setIntent(new Intent().putExtra("userId",3135930l));
        browser.onCreate(null);
        expect(browser.mUser.id).toEqual(3135930l);
        expect(browser.mUser.username).toEqual("SoundCloud Android @ MWC");
    }

    @Test @DisableStrictI18n
    public void testOnCreateOtherIdFetch() throws Exception {
        Robolectric.addHttpResponseRule("/users/"+USER_ID, TestHelper.resource(getClass(), "user.json"));
        Robolectric.getFakeHttpLayer().setDefaultHttpResponse(404, "");

        browser.setIntent(new Intent().putExtra("userId",3135930l));
        browser.onCreate(null);
        expect(browser.mUser.id).toEqual(3135930l);
        expect(browser.mUser.username).toEqual("SoundCloud Android @ MWC");
        expect(browser.mUser.country).toEqual("Germany");
    }

    @Test @DisableStrictI18n
    public void testOnCreateOtherModel() throws Exception {
        Robolectric.getFakeHttpLayer().setDefaultHttpResponse(404, "");

        browser.setIntent(new Intent().putExtra("user",getUser()));
        browser.onCreate(null);
        expect(browser.mUser.id).toEqual(3135930l);
        expect(browser.mUser.username).toBeNull();

        // test rest of lifecycle for crashes
        browser.onStart();
        browser.onResume();
    }

    @Test @DisableStrictI18n
    public void testOnCreateOtherModelFetch() throws Exception {
        Robolectric.addHttpResponseRule("/users/"+USER_ID, TestHelper.resource(getClass(), "user.json"));
        Robolectric.getFakeHttpLayer().setDefaultHttpResponse(404, "");

        browser.setIntent(new Intent().putExtra("user",getUser()));
        browser.onCreate(null);
        expect(browser.mUser.id).toEqual(3135930l);
        expect(browser.mUser.username).toEqual("SoundCloud Android @ MWC");
        expect(browser.mUser.country).toEqual("Germany");
    }

    private User getUserWithUsername(){
        User u = getUser();
        u.username = "SoundCloud Android @ MWC";
        return u;
    }

    private User getUser(){
        User u = new User();
        u.id = USER_ID;
        return u;
    }
}
