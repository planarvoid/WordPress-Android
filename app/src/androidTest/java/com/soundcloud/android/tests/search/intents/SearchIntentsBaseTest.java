package com.soundcloud.android.tests.search.intents;

import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;

public abstract class SearchIntentsBaseTest extends ActivityTest<SearchActivity> {

    public SearchIntentsBaseTest() {
        super(SearchActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(getIntent());
        super.setUp();
        waiter = new Waiter(solo);
    }

    protected abstract Intent getIntent();

}
