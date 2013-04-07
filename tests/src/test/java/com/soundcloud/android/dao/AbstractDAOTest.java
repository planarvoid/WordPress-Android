package com.soundcloud.android.dao;

import android.content.ContentResolver;
import com.soundcloud.android.model.ModelLike;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public abstract class AbstractDAOTest<T extends BaseDAO> {
    protected static final long USER_ID = 133201L;
    protected  ContentResolver resolver;

    protected T baseDAO;

    public AbstractDAOTest(T baseDAO) {
        this.baseDAO = baseDAO;
    }

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    protected T getDAO() {
        return baseDAO;
    }
}
