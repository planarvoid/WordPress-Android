package com.soundcloud.android.dao;

import android.content.ContentResolver;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public abstract class BaseDAOTest {
    protected static final long USER_ID = 133201L;
    protected  ContentResolver resolver;

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }
}
