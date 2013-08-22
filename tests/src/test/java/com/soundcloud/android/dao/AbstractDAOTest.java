package com.soundcloud.android.dao;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.runner.RunWith;

import android.content.ContentResolver;

@RunWith(DefaultTestRunner.class)
public abstract class AbstractDAOTest<T extends BaseDAO> {
    protected static final long OWNER_ID = 133201L;
    protected  ContentResolver resolver;

    protected T baseDAO;

    public AbstractDAOTest(T baseDAO) {
        this.baseDAO = baseDAO;
    }

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
        TestHelper.setUserId(OWNER_ID);
    }

    protected T getDAO() {
        return baseDAO;
    }
}
