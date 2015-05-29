package com.soundcloud.android.tests.search.intents;

import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.Waiter;

import android.content.Intent;

public abstract class SearchIntentsBaseTest extends ActivityTest<SearchActivity> {

    public SearchIntentsBaseTest() {
        super(SearchActivity.class);
    }

    @Override
    protected void logInHelper() {
        defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(getIntent());
        super.setUp();
        waiter = new Waiter(solo);
    }

    protected abstract Intent getIntent();

}
