package com.soundcloud.android.dao;

import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

public class UserDAOTest extends BaseDAOTest<UserDAO> {
    public UserDAOTest() {
        super(new UserDAO(Robolectric.application.getContentResolver()));
    }

    @Test
    public void test() throws Exception {
        // TODO: test previously untested methods!
    }

}
