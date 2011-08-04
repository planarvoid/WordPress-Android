package com.soundcloud.android.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.provider.DatabaseHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;

@RunWith(DefaultTestRunner.class)
public class UserTest {

    @Test
    public void testLocation() throws Exception {
        User u = new User();
        assertThat(u.getLocation(), equalTo(""));
        u.city = "Berlin";
        assertThat(u.getLocation(), equalTo("Berlin"));
        u.country = "Germany";
        assertThat(u.getLocation(), equalTo("Berlin, Germany"));
        u.city = null;
        assertThat(u.getLocation(), equalTo("Germany"));
    }

    @Test
    public void testBuildContentValues() throws Exception {
        User u = new User();
        u.buildContentValues(false);
        u.buildContentValues(true);
        u.id = 1000L;
        ContentValues cv = u.buildContentValues(false);
        assertThat(cv.getAsLong(DatabaseHelper.Users.ID), is(1000L));
    }
}
